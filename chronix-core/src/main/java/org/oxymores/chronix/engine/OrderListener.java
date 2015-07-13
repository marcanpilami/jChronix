/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.engine;

import java.util.Date;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.data.RunResult;
import org.oxymores.chronix.engine.helpers.Order;
import org.oxymores.chronix.engine.helpers.OrderType;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

class OrderListener extends BaseListener
{
    private static Logger log = Logger.getLogger(OrderListener.class);

    private MessageProducer jmsProducer;

    void startListening(Broker broker) throws JMSException
    {
        this.init(broker, true, true);
        log.debug(String.format("Initializing OrderListener"));

        // Register current object as a listener on ORDER queue
        this.qName = String.format(Constants.Q_ORDER, brokerName);
        this.jmsProducer = this.jmsSession.createProducer(null);
        this.subscribeTo(qName);
        if (broker.getCtx().getLocalNode().isConsole())
        {
            this.subscribeTo(Constants.Q_BOOTSTRAP);
        }
    }

    @Override
    public void onMessage(Message msg)
    {
        // Metadata
        try
        {
            if (msg.getStringProperty("BS") != null)
            {
                log.info("Received a metadata send order");
                orderSendMeta(((TextMessage) msg).getText(), msg.getJMSReplyTo(), msg.getJMSCorrelationID());
                jmsCommit();
                return;
            }
        }
        catch (JMSException ex)
        {
            log.error("could not read a text message", ex);
            jmsRollback();
            return;
        }

        // All other messages are object messages
        ObjectMessage omsg = (ObjectMessage) msg;
        Order order;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Order))
            {
                log.warn("An object was received on the order queue but was not an order! Ignored.");
                jmsCommit();
                return;
            }
            order = (Order) o;
        }
        catch (JMSException e)
        {
            log.error("An error occurred during order reception. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        log.info(String.format("An order was received. Type: %s", order.type));

        if (order.type == OrderType.RESTARTPJ)
        {
            orderRestart(order);
        }

        else if (order.type == OrderType.FORCEOK)
        {
            orderForceOk(order);
        }

        else if (order.type == OrderType.EXTERNAL)
        {
            orderExternal(order);
        }

        jmsCommit();
    }

    private void orderRestart(Order order)
    {
        // Find the PipelineJob
        PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);

        // Put the pipeline job inside the local pipeline
        try
        {
            String qName = String.format(Constants.Q_PJ, brokerName);
            log.info(String.format("A relaunch PJ will be sent for execution on queue %s", qName));
            Destination destination = this.jmsSession.createQueue(qName);
            ObjectMessage m = jmsSession.createObjectMessage(pj);
            jmsProducer.send(destination, m);
        }
        catch (Exception e)
        {
            log.error("An error occurred while processing a relaunch order. The order will be ignored", e);
        }
    }

    private void orderForceOk(Order order)
    {
        // Find the PipelineJob
        PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);
        if (pj != null && pj.getStatus().equals(Constants.JI_STATUS_DONE))
        {
            try
            {
                ActiveNodeBase a = pj.getActive(ctx);
                RunResult rr = a.forceOK();
                Event e = pj.createEvent(rr);
                SenderHelpers.sendEvent(e, jmsProducer, jmsSession, ctx, false);

                // Update history & PJ
                trTransac.begin();
                trHistory.begin();
                pj.setStatus(Constants.JI_STATUS_OVERRIDEN);
                RunLog rl = emHistory.find(RunLog.class, (String) order.data);
                if (rl != null)
                {
                    rl.setLastKnownStatus(Constants.JI_STATUS_OVERRIDEN);
                    rl.setLastLocallyModified(new Date());
                }
                trTransac.commit();
                trHistory.commit();
            }
            catch (Exception e)
            {
                log.error("An error occurred while processing a force OK order. The order will be ignored", e);
            }
        }
        else
        {
            if (pj == null)
            {
                log.error("Job does not exist - cannot be forced!");
            }
            else
            {
                log.error("The job cannot be forced: it is not in state DONE (current state: " + pj.getStatus() + ")");
            }
        }
    }

    private void orderExternal(Order order)
    {
        External e = null;
        Application a = null;
        for (Application app : ctx.getApplications())
        {
            for (External anb : app.getActiveElements(External.class))
            {
                if (anb.getName().equals((String) order.data))
                {
                    e = anb;
                    a = app;
                }
            }
        }

        if (e == null)
        {
            // destroy message - it's corrupt
            log.error(String.format("An order of type EXTERNAL was received but it was not refering to an existing external source (name was [%s], data was [%s]).", order.data,
                    order.data2));
            jmsCommit();
            return;
        }

        String d = null;
        if (order.data2 != null)
        {
            d = e.getCalendarString((String) order.data2);
        }
        log.debug(String.format("Pattern  is %s - String is %s - Result is %s", e.getRegularExpression(), order.data2, d));

        // Create an event for each State using this external event
        for (State s : a.getStates())
        {
            if (!s.getRepresents().equals(e))
            {
                continue;
            }
            Event evt = new Event();
            evt.setApplication(a);
            if (d != null && s.getCalendar() != null)
            {
                evt.setCalendar(s.getCalendar());
                evt.setCalendarOccurrenceID(s.getCalendar().getOccurrence(d).getId().toString());
            }
            evt.setConditionData1(0);
            evt.setLevel1IdU(new UUID(0, 1));
            evt.setLevel0IdU(s.getChain().getId());
            evt.setState(s);

            for (Place p : s.getRunsOn().getPlaces())
            {
                evt.setPlace(p);
                try
                {
                    log.debug("Sending event for external source");
                    SenderHelpers.sendEvent(evt, this.jmsProducer, this.jmsSession, this.ctx, false);
                }
                catch (JMSException e1)
                {
                    log.error("Could not create the events triggered by an external event. It will be ignored.", e1);
                }
            }
        }
    }

    private void orderSendMeta(String nodeName, Destination replyTo, String corelId)
    {
        // Does the node really exist?
        Network n = this.ctx.getNetwork();
        ExecutionNode en = n.getNode(nodeName);
        if (en == null)
        {
            try
            {
                log.warn("Received a metadata request from an unknown node");
                TextMessage msg = jmsSession.createTextMessage();
                msg.setText("Node [" + nodeName + "] does not exist");
                msg.setJMSCorrelationID(corelId);
                jmsProducer.send(replyTo, msg);
                return;
            }
            catch (JMSException e)
            {
                log.error("An error occurred while processing a metadata order. The order will be ignored", e);
            }
        }

        // Enqueue all applications using the standard queues and send network in the answer queue
        try
        {
            for (Application a : this.ctx.getApplications())
            {
                SenderHelpers.sendApplication(a, en, jmsProducer, jmsSession, false);
            }

            log.info(String.format("The network will be sent to node %s", nodeName));
            ObjectMessage m = jmsSession.createObjectMessage(n);
            m.setJMSCorrelationID(corelId);
            jmsProducer.send(replyTo, m);
        }
        catch (Exception e)
        {
            log.error("An error occurred while processing a metadata order. The order will be ignored", e);
        }
    }
}
