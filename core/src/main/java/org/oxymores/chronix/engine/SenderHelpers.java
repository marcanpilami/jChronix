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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.persistence.EntityManager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.exceptions.ChronixException;

public class SenderHelpers
{
	private static Logger log = Logger.getLogger(SenderHelpers.class);

	private static class JmsSendData
	{
		public MessageProducer jmsProducer;
		public Session jmsSession;
		public Connection connection;

		public JmsSendData(ChronixContext ctx)
		{
			try
			{
				// Factory
				ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://" + ctx.getBrokerName());

				// Connect to it...
				this.connection = factory.createConnection();
				this.connection.start();

				// Create session and producer
				this.jmsSession = connection.createSession(true, Session.SESSION_TRANSACTED);
				this.jmsProducer = jmsSession.createProducer(null);
			} catch (Exception e)
			{
			}
		}

		public void close()
		{
			try
			{
				this.jmsProducer.close();
				this.jmsSession.close();
				this.connection.close();
			} catch (JMSException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// Event
	private static void sendEvent(Event e, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException
	{
		String qName = String.format("Q.%s.EVENT", target.getBrokerName());
		log.info(String.format("An event will be sent over the wire on queue %s", qName));

		ObjectMessage m = jmsSession.createObjectMessage(e);

		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	public static void sendEvent(Event evt, MessageProducer jmsProducer, Session jmsSession, ChronixContext ctx, boolean commit)
			throws JMSException
	{
		State s = evt.getState(ctx);
		ArrayList<State> clientStates = s.getClientStates();

		// All client physical nodes
		ArrayList<ExecutionNode> clientPN = new ArrayList<ExecutionNode>();
		for (State st : clientStates)
		{
			for (ExecutionNode en : st.getRunsOnPhysicalNodes())
			{
				if (!clientPN.contains(en.getHost()))
					clientPN.add(en.getHost());
			}
		}

		// Send to all clients!
		for (ExecutionNode n : clientPN)
		{
			sendEvent(evt, n, jmsProducer, jmsSession, false);
		}

		// Commit globally
		if (commit)
			jmsSession.commit();
	}

	// Should only be used by tests (poor performances)
	public static void sendEvent(Event evt, ChronixContext ctx) throws JMSException
	{
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendEvent(evt, d.jmsProducer, d.jmsSession, ctx, true);

		// Cleanup
		d.close();
	}

	// Event
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// History

	public static void sendHistory(RunLog rl, ChronixContext ctx, MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException
	{
		// Always send both to ourselves and to the supervisor
		Application a = ctx.applicationsById.get(UUID.fromString(rl.applicationId));
		ExecutionNode self = a.getLocalNode();

		String qName = String.format("Q.%s.LOG", self.getBrokerName());
		log.info(String.format("A scheduler log will be sent on queue %s (%s)", qName, rl.activeNodeName));
		Destination destination = jmsSession.createQueue(qName);
		ObjectMessage m = jmsSession.createObjectMessage(rl);
		jmsProducer.send(destination, m);

		ExecutionNode console = ctx.applicationsById.get(UUID.fromString(rl.applicationId)).getConsoleNode();
		if (console != null && console != self)
		{
			qName = String.format("Q.%s.LOG", console.getBrokerName());
			log.info(String.format("A scheduler log will be sent on queue %s (%s)", qName, rl.activeNodeName));
			destination = jmsSession.createQueue(qName);
			m = jmsSession.createObjectMessage(rl);
			jmsProducer.send(destination, m);
		}

		if (commit)
			jmsSession.commit();
	}

	// History
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// Application

	// Should only be used by tests (poor performances)
	public static void sendApplication(Application a, ExecutionNode target, ChronixContext ctx) throws JMSException
	{
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendApplication(a, target, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendApplication(Application a, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException
	{
		String qName = String.format("Q.%s.APPLICATION", target.getBrokerName());
		log.info(String.format("An app will be sent over the wire on queue %s", qName));

		Destination destination = jmsSession.createQueue(qName);
		ObjectMessage m = jmsSession.createObjectMessage(a);
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	public static void sendApplicationToAllClients(Application a, ChronixContext ctx) throws JMSException
	{
		// Connect to the local broker
		JmsSendData d = new JmsSendData(ctx);

		// In case of misconfiguration, we may have double nodes.
		ArrayList<String> sent = new ArrayList<String>();

		for (ExecutionNode n : a.getNodesList())
		{
			if (n.isHosted())
				continue;
			if (sent.contains(n.getBrokerName()))
				continue;

			// Go
			SenderHelpers.sendApplication(a, n, d.jmsProducer, d.jmsSession, true);
			sent.add(n.getBrokerName());
		}

		// Cleanup
		d.close();
	}

	// Application
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// Send command to a runner agent directly (total shun of the engine)

	// Should only be used by tests (poor performances)
	public static void sendShellCommand(String cmd, ExecutionNode target, ChronixContext ctx) throws JMSException
	{
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendShellCommand(cmd, target, ctx, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendShellCommand(String cmd, ExecutionNode target, ChronixContext ctx, MessageProducer jmsProducer,
			Session jmsSession, boolean commit) throws JMSException
	{

		RunDescription rd = new RunDescription();
		rd.command = cmd;
		rd.outOfPlan = true;
		rd.Method = "Shell";

		String qName = String.format("Q.%s.RUNNER", target.getBrokerName());
		log.info(String.format("A command will be sent for execution on queue %s (%s)", qName, cmd));
		Destination destination = jmsSession.createQueue(qName);
		Destination replyTo = jmsSession.createQueue(String.format("Q.%s.ENDOFJOB", ctx.getBrokerName()));

		ObjectMessage m = jmsSession.createObjectMessage(rd);
		m.setJMSReplyTo(replyTo);
		m.setJMSCorrelationID(UUID.randomUUID().toString());
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	// Send command to a runner agent directly (total shun of the engine)
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// PipelineJob

	// Should only be used by tests (poor performances)
	public static void sendPipelineJobToRunner(PipelineJob pj, ExecutionNode target, ChronixContext ctx) throws JMSException
	{
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendPipelineJobToRunner(pj, target, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendPipelineJobToRunner(PipelineJob pj, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession,
			boolean commit) throws JMSException
	{
		String qName = String.format("Q.%s.PJ", target.getHost().getBrokerName());
		log.info(String.format("A job will be sent to the runner over the wire on queue %s", qName));
		Destination d = jmsSession.createQueue(qName);
		ObjectMessage om = jmsSession.createObjectMessage(pj);
		jmsProducer.send(d, om);

		if (commit)
			jmsSession.commit();
	}

	public static void runStateAlone(State s, Place p, ChronixContext ctx) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		s.runAlone(p, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlanWithoutCalendarUpdating(State s, Place p, ChronixContext ctx) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		s.runInsidePlanWithoutUpdatingCalendar(p, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlan(State s, ChronixContext ctx, EntityManager em) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		s.runInsidePlan(em, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlan(State s, Place p, ChronixContext ctx, EntityManager em) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		s.runInsidePlan(p, em, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	// PipelineJob
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// State calendar pointers
	public static void sendCalendarPointer(CalendarPointer cp, Calendar ca, Session jmsSession, MessageProducer jmsProducer, boolean commit)
			throws JMSException
	{
		// Send the updated CP to other execution nodes that may need it.
		List<State> states_using_calendar = ca.getUsedInStates();
		List<ExecutionNode> en_using_calendar = new ArrayList<ExecutionNode>();
		ExecutionNode tmp = null;
		for (State s : states_using_calendar)
		{
			for (Place p : s.getRunsOn().getPlaces())
			{
				tmp = p.getNode().getHost();
				if (!en_using_calendar.contains(tmp))
					en_using_calendar.add(tmp);
			}
		}

		// Add console to the list (needed for central calendar administration)
		ExecutionNode console = ca.getApplication().getConsoleNode();
		if (console != null && !en_using_calendar.contains(console))
			en_using_calendar.add(console);

		log.debug(String.format("The pointer should be sent to %s execution nodes (for %s possible customer state(s))",
				en_using_calendar.size(), states_using_calendar.size()));

		// Create message
		ObjectMessage m = jmsSession.createObjectMessage(cp);

		// Send the message to every client execution node
		for (ExecutionNode en : en_using_calendar)
		{
			String qName = String.format("Q.%s.CALENDARPOINTER", en.getBrokerName());
			log.info(String.format("A calendar pointer will be sent on queue %s", qName));
			Destination destination = jmsSession.createQueue(qName);
			jmsProducer.send(destination, m);
		}

		// Send
		if (commit)
			jmsSession.commit();
	}

	public static void sendCalendarPointer(CalendarPointer cp, Calendar ca, ChronixContext ctx) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		sendCalendarPointer(cp, ca, d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendCalendarPointerShift(EntityManager em, Integer shiftNext, Integer shiftCurrent, State s, Place p,
			ChronixContext ctx) throws Exception
	{
		CalendarPointer cp = s.getCurrentCalendarPointer(em, p);
		CalendarDay next = s.getCalendar().getOccurrenceShiftedBy(cp.getNextRunOccurrenceCd(ctx), shiftNext);
		CalendarDay current = s.getCalendar().getOccurrenceShiftedBy(cp.getLastEndedOccurrenceCd(ctx), shiftCurrent);
		cp.setNextRunOccurrenceCd(next);
		cp.setLastEndedOccurrenceCd(current);

		JmsSendData d = new JmsSendData(ctx);
		sendCalendarPointer(cp, s.getCalendar(), d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendCalendarPointerShift(Integer shiftNext, State s, ChronixContext ctx) throws Exception
	{
		sendCalendarPointerShift(shiftNext, 0, s, ctx);
	}

	public static void sendCalendarPointerShift(Integer shiftNext, Integer shiftCurrent, State s, ChronixContext ctx) throws Exception
	{
		EntityManager em = ctx.getTransacEM();
		for (Place p : s.getRunsOn().getPlaces())
			sendCalendarPointerShift(em, shiftNext, shiftCurrent, s, p, ctx);
	}

	public static void sendCalendarPointerShift(Integer shift, Calendar updatedCalendar, ChronixContext ctx) throws Exception
	{
		JmsSendData d = new JmsSendData(ctx);
		SenderHelpers.sendCalendarPointerShift(shift, updatedCalendar, ctx.getTransacEM(), ctx, d.jmsSession, d.jmsProducer, true);
		d.close();
	}

	public static void sendCalendarPointerShift(Integer shift, Calendar updatedCalendar, EntityManager em, ChronixContext ctx,
			Session jmsSession, MessageProducer jmsProducer, boolean commit) throws Exception
	{
		CalendarPointer cp = updatedCalendar.getCurrentOccurrencePointer(em);

		CalendarDay old_cd = updatedCalendar.getCurrentOccurrence(em);
		CalendarDay ref_cd = updatedCalendar.getOccurrenceShiftedBy(old_cd, shift - 1);

		CalendarDay new_cd = updatedCalendar.getOccurrenceAfter(ref_cd);
		CalendarDay next_cd = updatedCalendar.getOccurrenceAfter(new_cd);

		cp.setLastEndedOccurrenceCd(new_cd);
		cp.setLastEndedOkOccurrenceCd(new_cd);
		cp.setLastStartedOccurrenceCd(new_cd);
		cp.setNextRunOccurrenceCd(next_cd);

		SenderHelpers.sendCalendarPointer(cp, cp.getCalendar(ctx), jmsSession, jmsProducer, commit);
	}

	// /////////////////////////////////////////////////////////////////////////
	// restart orders
	public static void sendOrderRestartAfterFailure(String pipelineJobId, ExecutionNode en, Session jmsSession,
			MessageProducer jmsProducer, boolean commit) throws JMSException
	{

		Order o = new Order();
		o.type = OrderType.RESTARTPJ;
		o.data = pipelineJobId;

		// Create message
		ObjectMessage m = jmsSession.createObjectMessage(o);

		// Send the message to every client execution node
		String qName = String.format("Q.%s.ORDER", en.getHost().getBrokerName());
		log.info(String.format("An order will be sent on queue %s", qName));
		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		// Send
		if (commit)
			jmsSession.commit();
	}

	public static void sendOrderRestartAfterFailure(String pipelineJobId, ExecutionNode en, ChronixContext ctx) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		sendOrderRestartAfterFailure(pipelineJobId, en, d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendOrderForceOk(String pipelineJobId, ExecutionNode en, Session jmsSession, MessageProducer jmsProducer,
			boolean commit) throws JMSException
	{

		Order o = new Order();
		o.type = OrderType.FORCEOK;
		o.data = pipelineJobId;

		// Create message
		ObjectMessage m = jmsSession.createObjectMessage(o);

		// Send the message to every client execution node
		String qName = String.format("Q.%s.ORDER", en.getHost().getBrokerName());
		log.info(String.format("An order will be sent on queue %s", qName));
		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		// Send
		if (commit)
			jmsSession.commit();
	}

	public static void sendOrderForceOk(String appId, String pipelineJobId, String execNodeId, ChronixContext ctx) throws ChronixException
	{
		try
		{
			ExecutionNode en = ctx.applicationsById.get(UUID.fromString(appId)).getNode(UUID.fromString(execNodeId));
			sendOrderForceOk(pipelineJobId, en, ctx);
		} catch (Exception e)
		{
			throw new ChronixException("Cannot comply.", e);
		}
	}

	public static void sendOrderForceOk(String pipelineJobId, ExecutionNode en, ChronixContext ctx) throws JMSException
	{
		JmsSendData d = new JmsSendData(ctx);
		sendOrderForceOk(pipelineJobId, en, d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendOrderExternalEvent(UUID externalStateId, String data, ExecutionNode en, Session jmsSession,
			MessageProducer jmsProducer, boolean commit) throws JMSException
	{
		Order o = new Order();
		o.type = OrderType.EXTERNAL;
		o.data = externalStateId;
		o.data2 = data;

		// Create message
		ObjectMessage m = jmsSession.createObjectMessage(o);

		// Send the message to every client execution node
		String qName = String.format("Q.%s.ORDER", en.getHost().getBrokerName());
		log.info(String.format("An order will be sent on queue %s", qName));
		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		// Send
		if (commit)
			jmsSession.commit();
	}

	public static void sendOrderExternalEvent(UUID externalStateId, String data, ExecutionNode en, ChronixContext ctx) throws Exception
	{
		JmsSendData d = new JmsSendData(ctx);
		sendOrderExternalEvent(externalStateId, data, en, d.jmsSession, d.jmsProducer, false);
		d.jmsSession.commit();
		d.close();
	}

	// restart orders
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// Tokens
	public static void sendTokenRequest(TokenRequest tr, ChronixContext ctx, Session jmsSession, MessageProducer jmsProducer, boolean commit)
			throws JMSException
	{
		String qName = "";
		Application a = ctx.applicationsById.get(tr.applicationID);
		Place p = a.getPlace(tr.placeID);
		if (tr.local)
		{
			qName = String.format("Q.%s.TOKEN", a.getLocalNode().getBrokerName());
		}
		else
		{
			qName = String.format("Q.%s.TOKEN", p.getNode().getHost().getBrokerName());
		}

		// Return queue
		String localQueueName = String.format("Q.%s.TOKEN", a.getLocalNode().getBrokerName());
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
			jmsSession.commit();
	}
	// tokens
	// /////////////////////////////////////////////////////////////////////////
}
