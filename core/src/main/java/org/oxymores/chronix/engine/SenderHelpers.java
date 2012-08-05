package org.oxymores.chronix.engine;

import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.Event;

public class SenderHelpers {
	private static Logger log = Logger.getLogger(SenderHelpers.class);

	private static void sendEvent(Event e, ExecutionNode target,
			MessageProducer jmsProducer, Session jmsSession, boolean commit)
			throws JMSException {
		String qName = String.format("Q.%s.EVENT", target.getBrokerName());
		log.info(String.format(
				"An event will be sent over the wire on queue %s", qName));

		ObjectMessage m = jmsSession.createObjectMessage(e);

		Destination destination = jmsSession.createQueue(qName);
		jmsProducer.send(destination, m);

		if (commit)
			jmsSession.commit();
	}

	public static void sendEvent(Event evt, MessageProducer jmsProducer,
			Session jmsSession, ChronixContext ctx, boolean commit)
			throws JMSException {
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
	public static void sendEvent(Event evt, ChronixContext ctx)
			throws JMSException {
		// Factory
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(
				"vm://" + ctx.getBrokerName());

		// Connect to it...
		Connection connection = factory.createConnection();
		connection.start();

		// Create session and producer
		Session jmsSession = connection.createSession(true,
				Session.SESSION_TRANSACTED);
		MessageProducer jmsProducer = jmsSession.createProducer(null);

		// Go
		SenderHelpers.sendEvent(evt, jmsProducer, jmsSession, ctx, true);

		// Cleanup
		jmsProducer.close();
		jmsSession.close();
		connection.close();
	}
}
