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

import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.joda.time.DateTime;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.context.EngineCbRun;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.data.Order;
import org.oxymores.chronix.engine.data.OrderType;
import org.oxymores.chronix.engine.data.RunResult;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class OrderListener implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    private ChronixContextTransient ctxDb;
    private ChronixContextMeta ctxMeta;
    private String localNodeName;
    private ChronixEngine engine;

    OrderListener(ChronixEngine e, ChronixContextMeta ctxMeta, ChronixContextTransient ctxDb, String localNodeName)
    {
        this.ctxDb = ctxDb;
        this.ctxMeta = ctxMeta;
        this.localNodeName = localNodeName;
        this.engine = e;
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        // Metadata
        try
        {
            if (msg.getStringProperty("BS") != null)
            {
                log.info("Received a metadata send order");
                orderSendMeta(((TextMessage) msg).getText(), msg.getJMSReplyTo(), msg.getJMSCorrelationID(), jmsSession, jmsProducer);
                return;
            }
        }
        catch (JMSException ex)
        {
            log.error("could not read a text message", ex);
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
                return;
            }
            order = (Order) o;
        }
        catch (JMSException e)
        {
            throw new ListenerRollbackException(
                    "An error occurred during order reception. Message will stay in queue and will be analysed later", e);
        }

        log.info(String.format("An order was received. Type: %s", order.type));

        if (order.type == OrderType.RESTARTPJ)
        {
            orderRestart(order, jmsSession, jmsProducer);
        }

        else if (order.type == OrderType.FORCEOK)
        {
            orderForceOk(order, jmsSession, jmsProducer);
        }

        else if (order.type == OrderType.EXTERNAL)
        {
            orderExternal(order, jmsSession, jmsProducer);
        }
    }

    private void orderRestart(Order order, Session jmsSession, MessageProducer jmsProducer)
    {
        // Find the PipelineJob
        PipelineJob pj;
        try (Connection conn = this.ctxDb.getTransacDataSource().open())
        {
            pj = conn.createQuery("SELECT * FROM PipelineJob WHERE id=:id").addParameter("id", order.data)
                    .executeAndFetchFirst(PipelineJob.class);
        }

        // Put the pipeline job inside the local pipeline
        try
        {
            String qName = String.format(Constants.Q_PJ, localNodeName);
            log.info(String.format("A relaunch PJ will be sent for execution on queue %s", qName));
            Destination destination = jmsSession.createQueue(qName);
            ObjectMessage m = jmsSession.createObjectMessage(pj);
            jmsProducer.send(destination, m);
        }
        catch (Exception e)
        {
            log.error("An error occurred while processing a relaunch order. The order will be ignored", e);
        }
    }

    private void orderForceOk(Order order, Session jmsSession, MessageProducer jmsProducer)
    {
        // Find the PipelineJob
        PipelineJob pj = null;
        try (Connection conn = this.ctxDb.getTransacDataSource().open())
        {
            pj = conn.createQuery("SELECT * FROM PipelineJob WHERE id=:id").addParameter("id", order.data)
                    .executeAndFetchFirst(PipelineJob.class);
            if (pj == null)
            {
                log.error("Job does not exist - cannot be forced!");
                return;
            }
            pj.getEnvValues(conn);
        }

        if (pj.getStatus().equals(Constants.JI_STATUS_DONE))
        {
            try
            {
                EventSourceDef a = pj.getActive(ctxMeta);
                RunResult rr = a.forceOK(new EngineCbRun(engine, this.ctxMeta, pj.getApplication(ctxMeta), pj), pj);
                Event e = pj.createEvent(rr, pj.getVirtualTime(), this.ctxMeta);
                SenderHelpers.sendEvent(e, jmsProducer, jmsSession, ctxMeta, false);

                // Update history & PJ
                try (Connection connHist = this.ctxDb.getHistoryDataSource().beginTransaction();
                        Connection connTransac = this.ctxDb.getTransacDataSource().beginTransaction())
                {
                    pj.setStatus(Constants.JI_STATUS_OVERRIDEN);
                    pj.insertOrUpdate(connTransac);

                    RunLog rl = connHist.createQuery("SELECT * FROM RunLog WHERE id=:id").addParameter("id", order.data)
                            .executeAndFetchFirst(RunLog.class);
                    if (rl != null)
                    {
                        rl.setLastKnownStatus(Constants.JI_STATUS_OVERRIDEN);
                        rl.setLastLocallyModified(DateTime.now());
                        SenderHelpers.sendHistory(rl, ctxMeta, jmsProducer, jmsSession, true, localNodeName);
                    }
                }
            }
            catch (Exception e)
            {
                log.error("An error occurred while processing a force OK order. The order will be ignored", e);
            }
        }
        else
        {
            log.error("The job cannot be forced: it is not in state DONE (current state: " + pj.getStatus() + ")");
        }
    }

    private void orderExternal(Order order, Session jmsSession, MessageProducer jmsProducer)
    {
        EventSourceDef e = null;
        Application a = null;
        for (Application app : ctxMeta.getApplications())
        {
            try
            {
                e = app.getEventSourceByName((String) order.data);
                a = app;
                break;
            }
            catch (ChronixException ex)
            {
                continue;
            }
        }

        if (e == null)
        {
            // destroy message - it's corrupt
            log.error(String.format(
                    "An order of type EXTERNAL was received but it was not refering to an existing external source (name was [%s], data was [%s]).",
                    order.data, order.data2));
            return;
        }

        Map<String, String> args = new java.util.HashMap<>();
        args.put("externalData", (String) order.data2);

        // Just launch the source on all its states. It does not matter if it is local or not - externals are supposed to be
        // node-agnostic.
        try (Connection connTransac = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            for (State s : a.getStatesClientOfSource(e.getId()))
            {
                s.runInsidePlan(connTransac, jmsProducer, jmsSession, null, null, DateTime.now(), args);
            }
        }
        catch (Exception e1)
        {
            // weird. We may want to retry later.
            throw new ListenerRollbackException("could not relay external order", e1);
        }
    }

    private void orderSendMeta(String nodeName, Destination replyTo, String corelId, Session jmsSession, MessageProducer jmsProducer)
    {
        // Does the node really exist?
        Environment n = this.ctxMeta.getEnvironment();
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

        // Enqueue all applications using the standard queues and send environment in the answer queue
        try
        {
            int nbApps = this.ctxMeta.getApplications().size();
            int nbSent = 0;
            for (Application a : this.ctxMeta.getApplications())
            {
                nbSent++;
                SenderHelpers.sendApplication(a, en, jmsProducer, jmsSession, false, nbSent != nbApps);
            }

            log.info(String.format("The environment will be sent to node %s", nodeName));
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
