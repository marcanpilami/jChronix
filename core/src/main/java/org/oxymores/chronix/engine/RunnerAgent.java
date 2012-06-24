package org.oxymores.chronix.engine;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.transactional.Event;

public class RunnerAgent implements MessageListener {

	private static Logger log = Logger.getLogger(RunnerAgent.class);
	private Session session;
	private Destination dest;
	private Connection cnx;

	public void startListening(Connection cnx, String brokerName)
			throws JMSException {
		this.cnx = cnx;
		String qName = String.format("Q.%s.RUNNER", brokerName);
		log.debug(String.format(
				"Broker %s: registering a runner listener on queue %s",
				brokerName, qName));
		this.session = this.cnx.createSession(true, Session.SESSION_TRANSACTED);
		this.dest = this.session.createQueue(qName);
		MessageConsumer consumer = this.session.createConsumer(dest);
		consumer.setMessageListener(this);
	}

	@Override
	public void onMessage(Message msg) {

		String cmd;
		ObjectMessage omsg = (ObjectMessage) msg;
		RunDescription rd;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof RunDescription)) {
				log.warn("An object was received on the runner queue but was not a RunDescription! Ignored.");
				commit();
				return;
			}
			rd = (RunDescription) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during RunDescription reception. BAD. Message will stay in queue and will be analysed later",
					e);
			rollback();
			return;
		}

		log.info(String.format("Running command %s", rd.command));
		try {
			session.commit();
		} catch (JMSException e) {
			log.error("oups", e);
		}
	}
	
	private void commit() {
		try {
			session.commit();
		} catch (JMSException e) {
			log.error(
					"failure to acknowledge a message in the JMS queue. Scheduler will now abort as it is a dangerous situation.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void rollback() {
		try {
			session.rollback();
		} catch (JMSException e) {
			log.error(
					"failure to rollback an message in the JMS queue. Scheduler will now abort as it is a dangerous situation.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

}
