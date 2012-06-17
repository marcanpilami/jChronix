package org.oxymores.chronix.engine;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

public class RunnerListener implements MessageListener {

	private static Logger log = Logger.getLogger(RunnerListener.class);
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
		try {
			if (!(msg instanceof TextMessage)) {
				log.warn("An object was received on the runner queue but was not a String! Ignored.");
				session.commit();
				return;
			}
			TextMessage omsg = (TextMessage) msg;
			cmd = omsg.getText();
		} catch (JMSException e) {
			log.error(
					"An error occurred during runner command reception. BAD. Message will stay in queue and will be analysed later",
					e);
			try {
				session.rollback();
			} catch (JMSException e1) {
			}
			return;
		}

		log.info(String.format("Running command %s", cmd));
		try {
			session.commit();
		} catch (JMSException e) {
			log.error("oups", e);
		}
	}

}
