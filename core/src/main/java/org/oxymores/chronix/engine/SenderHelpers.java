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
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class SenderHelpers {
	private static Logger log = Logger.getLogger(SenderHelpers.class);

	private static class JmsSendData {
		public MessageProducer jmsProducer;
		public Session jmsSession;
		public Connection connection;

		public JmsSendData(ChronixContext ctx) {
			try {
				// Factory
				ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory("vm://" + ctx.getBrokerName());

				// Connect to it...
				this.connection = factory.createConnection();
				this.connection.start();

				// Create session and producer
				this.jmsSession = connection.createSession(true, Session.SESSION_TRANSACTED);
				this.jmsProducer = jmsSession.createProducer(null);
			} catch (Exception e) {
			}
		}

		public void close() {
			try {
				this.jmsProducer.close();
				this.jmsSession.close();
				this.connection.close();
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// Event
	private static void sendEvent(Event e, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit) throws JMSException {
		String qName = String.format("Q.%s.EVENT", target.getBrokerName());
		log.info(String.format("An event will be sent over the wire on queue %s", qName));

		ObjectMessage m = jmsSession.createObjectMessage(e);

		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	public static void sendEvent(Event evt, MessageProducer jmsProducer, Session jmsSession, ChronixContext ctx, boolean commit) throws JMSException {
		State s = evt.getState(ctx);
		ArrayList<State> clientStates = s.getClientStates();

		// All client physical nodes
		ArrayList<ExecutionNode> clientPN = new ArrayList<ExecutionNode>();
		for (State st : clientStates) {
			for (ExecutionNode en : st.getRunsOnPhysicalNodes()) {
				if (!clientPN.contains(en))
					clientPN.add(en);
			}
		}

		// Send to all clients!
		for (ExecutionNode n : clientPN) {
			sendEvent(evt, n, jmsProducer, jmsSession, false);
		}

		// Commit globally
		if (commit)
			jmsSession.commit();
	}

	// Should only be used by tests (poor performances)
	public static void sendEvent(Event evt, ChronixContext ctx) throws JMSException {
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
	// Application

	// Should only be used by tests (poor performances)
	public static void sendApplication(Application a, ExecutionNode target, ChronixContext ctx) throws JMSException {
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendApplication(a, target, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendApplication(Application a, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException {
		String qName = String.format("Q.%s.APPLICATION", target.getBrokerName());
		log.info(String.format("An app will be sent over the wire on queue %s", qName));

		Destination destination = jmsSession.createQueue(qName);
		ObjectMessage m = jmsSession.createObjectMessage(a);
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	// Application
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// Send command to a runner agent directly (total shun of the engine)

	// Should only be used by tests (poor performances)
	public static void sendShellCommand(String cmd, ExecutionNode target, ChronixContext ctx) throws JMSException {
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendShellCommand(cmd, target, ctx, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendShellCommand(String cmd, ExecutionNode target, ChronixContext ctx, MessageProducer jmsProducer, Session jmsSession,
			boolean commit) throws JMSException {

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
	public static void sendPipelineJobToRunner(PipelineJob pj, ExecutionNode target, ChronixContext ctx) throws JMSException {
		// Connect to a broker
		JmsSendData d = new JmsSendData(ctx);

		// Go
		SenderHelpers.sendPipelineJobToRunner(pj, target, d.jmsProducer, d.jmsSession, true);

		// Cleanup
		d.close();
	}

	public static void sendPipelineJobToRunner(PipelineJob pj, ExecutionNode target, MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException {
		String qName = String.format("Q.%s.PJ", target.getHost().getBrokerName());
		log.info(String.format("A job will be sent to the runner over the wire on queue %s", qName));
		Destination d = jmsSession.createQueue(qName);
		ObjectMessage om = jmsSession.createObjectMessage(pj);
		jmsProducer.send(d, om);

		if (commit)
			jmsSession.commit();
	}

	public static void runStateAlone(State s, Place p, ChronixContext ctx) throws JMSException {
		JmsSendData d = new JmsSendData(ctx);
		s.runAlone(p, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlanWithoutCalendarUpdating(State s, Place p, ChronixContext ctx) throws JMSException {
		JmsSendData d = new JmsSendData(ctx);
		s.runInsidePlanWithoutUpdatingCalendar(p, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlan(State s, ChronixContext ctx, EntityManager em) throws JMSException {
		JmsSendData d = new JmsSendData(ctx);
		s.runInsidePlan(em, d.jmsProducer, d.jmsSession);
		d.jmsSession.commit();
		d.close();
	}

	public static void runStateInsidePlan(State s, Place p, ChronixContext ctx, EntityManager em) throws JMSException {
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
			throws JMSException {
		// Send the updated CP to other execution nodes that may need it.
		List<State> states_using_calendar = ca.getUsedInStates();
		List<ExecutionNode> en_using_calendar = new ArrayList<ExecutionNode>();
		ExecutionNode tmp = null;
		for (State s : states_using_calendar) {
			for (Place p : s.getRunsOn().getPlaces()) {
				tmp = p.getNode().getHost();
				if (!en_using_calendar.contains(tmp))
					en_using_calendar.add(tmp);
			}
		}

		// Add console to the list (needed for central calendar administration)
		ExecutionNode console = ca.getApplication().getConsoleNode();
		if (console != null && !en_using_calendar.contains(console))
			en_using_calendar.add(console);

		log.debug(String.format("The pointer should be sent to %s execution nodes (for %s possible customer state(s))", en_using_calendar.size(),
				states_using_calendar.size()));

		// Create message
		ObjectMessage m = jmsSession.createObjectMessage(cp);

		// Send the message to every client execution node
		for (ExecutionNode en : en_using_calendar) {
			String qName = String.format("Q.%s.CALENDARPOINTER", en.getBrokerName());
			log.info(String.format("A calendar pointer will be sent on queue %s", qName));
			Destination destination = jmsSession.createQueue(qName);
			jmsProducer.send(destination, m);
		}

		// Send
		if (commit)
			jmsSession.commit();
	}

	public static void sendCalendarPointer(CalendarPointer cp, Calendar ca, ChronixContext ctx) throws JMSException {
		JmsSendData d = new JmsSendData(ctx);
		sendCalendarPointer(cp, ca, d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendCalendarPointerShift(EntityManager em, Integer shift, State s, Place p, ChronixContext ctx) throws Exception {
		CalendarPointer cp = s.getCurrentCalendarPointer(em, p);
		CalendarDay next = s.getCalendar().getOccurrenceAfter(cp.getNextRunOccurrenceCd(ctx));
		cp.setNextRunOccurrenceCd(next);

		JmsSendData d = new JmsSendData(ctx);
		sendCalendarPointer(cp, s.getCalendar(), d.jmsSession, d.jmsProducer, true);
		d.jmsSession.commit();
		d.close();
	}

	public static void sendCalendarPointerShift(Integer shift, State s, ChronixContext ctx) throws Exception {
		EntityManager em = ctx.getTransacEM();
		for (Place p : s.getRunsOn().getPlaces())
			sendCalendarPointerShift(em, shift, s, p, ctx);
	}
	// State calendar pointers
	// /////////////////////////////////////////////////////////////////////////
}
