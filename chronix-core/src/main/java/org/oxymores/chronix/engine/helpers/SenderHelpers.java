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
package org.oxymores.chronix.engine.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.FunctionalOccurrence;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.data.Order;
import org.oxymores.chronix.engine.data.OrderType;
import org.oxymores.chronix.engine.data.RunResult;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.exceptions.ChronixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All JMS Message sending is done here.<br>
 * This class only connects to the local broker - it is the local broker's responsibility to actually route messages to the right node.
 */
public class SenderHelpers
{
    private static Logger log = LoggerFactory.getLogger(SenderHelpers.class);
    public static String defaultBrokerName;

    private SenderHelpers()
    {

    }

    public static class JmsSendData implements AutoCloseable
    {
        public MessageProducer jmsProducer;
        public Session jmsSession;
        Connection connection;

        public JmsSendData() throws JMSException
        {
            this("vm://" + defaultBrokerName + "?create=false");
        }

        private JmsSendData(String brokerUrl) throws JMSException
        {
            // Factory
            ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);

            // Connect to it...
            this.connection = factory.createConnection();
            this.connection.start();

            // Create session and producer
            this.jmsSession = connection.createSession(true, Session.SESSION_TRANSACTED);
            this.jmsProducer = jmsSession.createProducer(null);
        }

        @Override
        public void close()
        {
            if (this.jmsSession != null)
            {
                try
                {
                    this.jmsSession.commit();
                }
                catch (Exception e)
                {
                    log.warn("An error occurred while commmiting a JMS connection. Just a warning, something may be weird in the network",
                            e);
                }
            }

            try
            {
                this.jmsProducer.close();
                this.jmsSession.close();
                this.connection.close();
            }
            catch (JMSException e)
            {
                log.warn("An error occurred while closing a JMS connection. Just a warning, something may be weird in the network", e);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event
    private static void sendEvent(Event e, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
            throws JMSException
    {
        String qName = String.format(Constants.Q_EVENT, target.getName());
        log.info(String.format("An event will be sent over the wire on queue %s", qName));

        ObjectMessage m = jmsSession.createObjectMessage(e);

        Destination destination = jmsSession.createQueue(qName);
        jmsProducer.send(destination, m);

        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendEvent(Event evt, MessageProducer jmsProducer, Session jmsSession, ChronixContextMeta ctx, boolean commit)
            throws JMSException
    {
        State s = evt.getState(ctx);
        List<State> clientStates = s.getClientStates();

        // All client physical nodes
        List<ExecutionNode> clientPN = new ArrayList<>();
        for (State st : clientStates)
        {
            for (ExecutionNode en : st.getRunsOnPhysicalNodes())
            {
                if (!clientPN.contains(en.getComputingNode()))
                {
                    clientPN.add(en.getComputingNode());
                }
            }
        }

        // Send to all clients!
        for (ExecutionNode n : clientPN)
        {
            sendEvent(evt, n, jmsProducer, jmsSession, false);
        }

        // Commit globally
        if (commit)
        {
            jmsSession.commit();
        }
    }

    // Should only be used by tests (poor performances)
    public static void sendEvent(Event evt, ChronixContextMeta ctx) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            SenderHelpers.sendEvent(evt, d.jmsProducer, d.jmsSession, ctx, true);
        }
    }

    // Event
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // History
    public static void sendHistory(RunLog rl, ChronixContextMeta ctx, MessageProducer jmsProducer, Session jmsSession, boolean commit,
            String localNodeName) throws JMSException
    {
        // Always send both to local node and to the supervisor
        String qName = String.format(Constants.Q_LOG, localNodeName);
        log.info(String.format("A scheduler log will be sent to the responsible engine on queue %s (%s)", qName, rl.getActiveNodeName()));
        Destination destination = jmsSession.createQueue(qName);
        ObjectMessage m = jmsSession.createObjectMessage(rl);
        jmsProducer.send(destination, m);

        ExecutionNode console = ctx.getEnvironment().getConsoleNode();
        if (console != null && !console.getName().equals(localNodeName))
        {
            qName = String.format(Constants.Q_LOG, console.getName());
            log.info(String.format("A scheduler log will be sent to the console on queue %s (%s)", qName, rl.getActiveNodeName()));
            destination = jmsSession.createQueue(qName);
            m = jmsSession.createObjectMessage(rl);
            jmsProducer.send(destination, m);
        }

        if (commit)
        {
            jmsSession.commit();
        }
    }

    // History
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Application
    // Should only be used by tests (poor performances)
    public static void sendApplication(Application a, ExecutionNode target) throws JMSException
    {
        // Connect to a broker
        try (JmsSendData d = new JmsSendData())
        {
            SenderHelpers.sendApplication(a, target, d.jmsProducer, d.jmsSession, true, false);
        }
    }

    public static void sendApplication(Application a, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit,
            boolean dontRestart) throws JMSException
    {
        String qName = String.format(Constants.Q_META, target.getName());
        log.info(String.format("An app will be sent over the wire on queue %s", qName));

        Destination destination = jmsSession.createQueue(qName);
        ObjectMessage m = jmsSession.createObjectMessage(a);
        m.setBooleanProperty("dont_restart", dontRestart);
        jmsProducer.send(destination, m);

        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendApplicationToAllClients(Application a, ChronixContextMeta ctx) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            // In case of misconfiguration, we may have double nodes.
            ArrayList<String> sent = new ArrayList<>();

            for (ExecutionNode n : ctx.getEnvironment().getNodesList())
            {
                if (n.isHosted() || sent.contains(n.getBrokerName()))
                {
                    continue;
                }

                // Go
                SenderHelpers.sendApplication(a, n, d.jmsProducer, d.jmsSession, true, false);
                sent.add(n.getBrokerName());
            }
        }
    }

    // Application
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Environment
    public static void sendEnvironment(Environment n, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
            throws JMSException
    {
        String qName = String.format(Constants.Q_META, target.getName());
        log.info(String.format("An environment specification will be sent over the wire on queue %s", qName));

        Destination destination = jmsSession.createQueue(qName);
        ObjectMessage m = jmsSession.createObjectMessage(n);
        jmsProducer.send(destination, m);

        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendEnvironmentToAllNodes(Environment n) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            // In case of misconfiguration, we may have double nodes.
            ArrayList<String> sent = new ArrayList<>();

            for (ExecutionNode en : n.getNodesList())
            {
                if (en.isHosted() || sent.contains(en.getBrokerName()))
                {
                    continue;
                }

                // Go
                SenderHelpers.sendEnvironment(n, en, d.jmsProducer, d.jmsSession, false);
                sent.add(en.getBrokerName());
            }
        }
    }

    // Environment
    // /////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////
    // PipelineJob
    // Should only be used by tests (poor performances)
    public static void sendToPipeline(PipelineJob pj, ExecutionNode target) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            SenderHelpers.sendToPipeline(pj, target, d.jmsProducer, d.jmsSession, true);
        }
    }

    public static void sendToPipeline(PipelineJob pj, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
            throws JMSException
    {
        String qName = String.format(Constants.Q_PJ, target.getComputingNode().getName());
        log.info(String.format("A job will be sent to the runner over the wire on queue %s", qName));
        Destination d = jmsSession.createQueue(qName);
        ObjectMessage om = jmsSession.createObjectMessage(pj);
        jmsProducer.send(d, om);

        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void runStateAlone(State s, Place p) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            s.runAlone(p, d.jmsProducer, d.jmsSession);
        }
    }

    public static void runStateInsidePlanWithoutCalendarUpdating(State s, Place p) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            s.runInsidePlanWithoutUpdatingCalendar(p, d.jmsProducer, d.jmsSession, DateTime.now());
        }
    }

    public static void runStateInsidePlan(State s, org.sql2o.Connection conn, Map<String, String> fieldOverload) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            s.runInsidePlan(conn, d.jmsProducer, d.jmsSession, null, null, DateTime.now(), fieldOverload);
        }
    }

    public static void runStateInsidePlan(State s, Place p, org.sql2o.Connection conn, Map<String, String> fieldOverload)
            throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            s.runInsidePlan(p, conn, d.jmsProducer, d.jmsSession, DateTime.now(), fieldOverload);
        }
    }

    // PipelineJob
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // State calendar pointers
    public static void sendCalendarPointer(CalendarPointer cp, FunctionalSequence ca, Session jmsSession, MessageProducer jmsProducer,
            boolean commit, Environment envt) throws JMSException
    {
        // Send the updated CP to other execution nodes that may need it.
        List<State> statesUsingCalendar = ca.getUsedInStates();
        List<ExecutionNode> enUsingCalendar = new ArrayList<>();
        ExecutionNode tmp;
        for (State s : statesUsingCalendar)
        {
            for (Place p : s.getRunsOn().getPlaces())
            {
                tmp = p.getNode().getComputingNode();
                if (!enUsingCalendar.contains(tmp))
                {
                    enUsingCalendar.add(tmp);
                }
            }
        }

        // Add console to the list (needed for central calendar administration)
        ExecutionNode console = envt.getConsoleNode();
        if (console != null && !enUsingCalendar.contains(console))
        {
            enUsingCalendar.add(console);
        }

        log.debug(String.format("The pointer should be sent to %s execution nodes (for %s possible customer state(s))",
                enUsingCalendar.size(), statesUsingCalendar.size()));

        // Create message
        ObjectMessage m = jmsSession.createObjectMessage(cp);

        // Send the message to every client execution node
        for (ExecutionNode en : enUsingCalendar)
        {
            String qName = String.format(Constants.Q_CALENDARPOINTER, en.getName());
            log.info(String.format("A calendar pointer will be sent on queue %s", qName));
            Destination destination = jmsSession.createQueue(qName);
            jmsProducer.send(destination, m);
        }

        // Send
        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendCalendarPointer(CalendarPointer cp, FunctionalSequence ca, Environment envt) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            sendCalendarPointer(cp, ca, d.jmsSession, d.jmsProducer, true, envt);
        }
    }

    public static void sendCalendarPointerShift(org.sql2o.Connection conn, Integer shiftNext, Integer shiftCurrent, State s, Place p,
            ChronixContextMeta ctx) throws JMSException
    {
        CalendarPointer cp = s.getCurrentCalendarPointer(conn, p);
        FunctionalOccurrence next = s.getCalendar().getOccurrenceShiftedBy(cp.getNextRunOccurrenceCd(ctx), shiftNext);
        FunctionalOccurrence current = s.getCalendar().getOccurrenceShiftedBy(cp.getLastEndedOccurrenceCd(ctx), shiftCurrent);
        cp.setNextRunOccurrenceCd(next);
        cp.setLastEndedOccurrenceCd(current);

        try (JmsSendData d = new JmsSendData())
        {
            sendCalendarPointer(cp, s.getCalendar(), d.jmsSession, d.jmsProducer, true, ctx.getEnvironment());
        }
    }

    public static void sendCalendarPointerShift(Integer shiftNext, State s, ChronixContextMeta ctx, ChronixContextTransient ctx2)
            throws JMSException
    {
        sendCalendarPointerShift(shiftNext, 0, s, ctx, ctx2);
    }

    public static void sendCalendarPointerShift(Integer shiftNext, Integer shiftCurrent, State s, ChronixContextMeta ctx,
            ChronixContextTransient ctx2) throws JMSException
    {
        try (org.sql2o.Connection conn = ctx2.getTransacDataSource().open())
        {
            for (Place p : s.getRunsOn().getPlaces())
            {
                sendCalendarPointerShift(conn, shiftNext, shiftCurrent, s, p, ctx);
            }
        }
    }

    public static void sendCalendarPointerShift(Integer shift, FunctionalSequence updatedCalendar, ChronixContextMeta ctx,
            ChronixContextTransient ctxDb) throws JMSException
    {
        try (JmsSendData d = new JmsSendData(); org.sql2o.Connection conn = ctxDb.getTransacDataSource().open())
        {
            SenderHelpers.sendCalendarPointerShift(shift, updatedCalendar, conn, ctx, d.jmsSession, d.jmsProducer, true);
        }
    }

    public static void sendCalendarPointerShift(Integer shift, FunctionalSequence updatedCalendar, org.sql2o.Connection conn,
            ChronixContextMeta ctx, Session jmsSession, MessageProducer jmsProducer, boolean commit) throws JMSException
    {
        CalendarPointer cp = updatedCalendar.getCurrentOccurrencePointer(conn);

        FunctionalOccurrence oldCd = updatedCalendar.getCurrentOccurrence(conn);
        FunctionalOccurrence refCd = updatedCalendar.getOccurrenceShiftedBy(oldCd, shift - 1);

        FunctionalOccurrence newCd = updatedCalendar.getOccurrenceAfter(refCd);
        FunctionalOccurrence nextCd = updatedCalendar.getOccurrenceAfter(newCd);

        cp.setLastEndedOccurrenceCd(newCd);
        cp.setLastEndedOkOccurrenceCd(newCd);
        cp.setLastStartedOccurrenceCd(newCd);
        cp.setNextRunOccurrenceCd(nextCd);

        SenderHelpers.sendCalendarPointer(cp, cp.getCalendar(ctx), jmsSession, jmsProducer, commit, ctx.getEnvironment());
    }

    ///////////////////////////////////////////////////////////////////////////
    // restart orders
    public static void sendOrderRestartAfterFailure(UUID pipelineJobId, ExecutionNode en, Session jmsSession, MessageProducer jmsProducer,
            boolean commit) throws JMSException
    {
        Order o = new Order();
        o.type = OrderType.RESTARTPJ;
        o.data = pipelineJobId;

        // Create message
        ObjectMessage m = jmsSession.createObjectMessage(o);

        // Send the message to every client execution node
        String qName = String.format(Constants.Q_ORDER, en.getComputingNode().getName());
        log.info(String.format("A restart order will be sent on queue %s", qName));
        Destination destination = jmsSession.createQueue(qName);
        jmsProducer.send(destination, m);

        // Send
        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendOrderRestartAfterFailure(UUID pipelineJobId, ExecutionNode en) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            sendOrderRestartAfterFailure(pipelineJobId, en, d.jmsSession, d.jmsProducer, true);
        }
    }

    public static void sendOrderForceOk(UUID pipelineJobId, ExecutionNode en, Session jmsSession, MessageProducer jmsProducer,
            boolean commit) throws JMSException
    {
        Order o = new Order();
        o.type = OrderType.FORCEOK;
        o.data = pipelineJobId;

        // Create message
        ObjectMessage m = jmsSession.createObjectMessage(o);

        // Send the message to every client execution node
        String qName = String.format(Constants.Q_ORDER, en.getComputingNode().getName());
        log.info(String.format("A force OK order will be sent on queue %s", qName));
        Destination destination = jmsSession.createQueue(qName);
        jmsProducer.send(destination, m);

        // Send
        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendOrderForceOk(UUID appId, UUID pipelineJobId, UUID execNodeId, Environment envt) throws ChronixException
    {
        try
        {
            ExecutionNode en = envt.getNode(execNodeId);
            sendOrderForceOk(pipelineJobId, en);
        }
        catch (Exception e)
        {
            throw new ChronixException("Cannot comply.", e);
        }
    }

    public static void sendOrderForceOk(UUID pipelineJobId, ExecutionNode en) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            sendOrderForceOk(pipelineJobId, en, d.jmsSession, d.jmsProducer, true);
        }
    }

    public static void sendOrderExternalEvent(String sourceName, String data, String nodeName, Session jmsSession,
            MessageProducer jmsProducer, boolean commit) throws JMSException
    {
        Order o = new Order();
        o.type = OrderType.EXTERNAL;
        o.data = sourceName;
        o.data2 = data;

        // Create message
        ObjectMessage m = jmsSession.createObjectMessage(o);

        // Send the message to every client execution node
        String qName = String.format(Constants.Q_ORDER, nodeName);
        log.info(String.format("An external event order will be sent on queue %s", qName));
        Destination destination = jmsSession.createQueue(qName);
        jmsProducer.send(destination, m);

        // Send
        if (commit)
        {
            jmsSession.commit();
        }
    }

    public static void sendOrderExternalEvent(String sourceName, String data, String nodeName) throws JMSException
    {
        try (JmsSendData d = new JmsSendData())
        {
            sendOrderExternalEvent(sourceName, data, nodeName, d.jmsSession, d.jmsProducer, false);
        }
    }

    // restart orders
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Tokens
    public static void sendTokenRequest(TokenRequest tr, ChronixContextMeta ctx, Session jmsSession, MessageProducer jmsProducer,
            boolean commit, String brokerName) throws JMSException
    {
        String qName;
        Place p = ctx.getEnvironment().getPlace(tr.placeID);
        if (tr.local)
        {
            qName = String.format(Constants.Q_TOKEN, brokerName);
        }
        else
        {
            qName = String.format(Constants.Q_TOKEN, p.getNode().getComputingNode().getBrokerName());
        }

        // Return queue
        String localQueueName = String.format(Constants.Q_TOKEN, brokerName);
        Destination returnQueue = jmsSession.createQueue(localQueueName);

        // Create message
        ObjectMessage m = jmsSession.createObjectMessage(tr);
        m.setJMSReplyTo(returnQueue);
        m.setJMSCorrelationID(tr.stateID.toString() + tr.placeID.toString());

        // Send the message to its target
        log.info(String.format("A token request will be sent on queue %s", qName));
        Destination destination = jmsSession.createQueue(qName);
        jmsProducer.send(destination, m);

        // Send
        if (commit)
        {
            jmsSession.commit();
        }
    }
    // tokens
    ///////////////////////////////////////////////////////////////////////////

    // Only works with local node. Always commit.
    public static void sendRunResult(RunResult rr, String nodeName) throws JMSException
    {
        JmsSendData d = new JmsSendData();
        String qName = String.format(Constants.Q_RUNNERMGR, nodeName);

        ObjectMessage m = d.jmsSession.createObjectMessage(rr);
        Destination destination = d.jmsSession.createQueue(qName);
        d.jmsProducer.send(destination, m);
        d.jmsSession.commit();
        d.close();
    }
}
