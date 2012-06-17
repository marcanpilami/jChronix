package org.oxymores.chronix.engine;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.Event;

public class EventListener implements MessageListener {

	private static Logger log = Logger.getLogger(EventListener.class);

	@SuppressWarnings("unused")
	private ChronixContext ctx;
	private Session session;
	private Destination dest;
	private Connection cnx;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx)
			throws JMSException {
		this.ctx = ctx;
		this.cnx = cnx;
		String qName = String.format("Q.%s.EVENT", brokerName);
		log.debug(String.format(
				"Broker %s: registering an event listener on queue %s",
				brokerName, qName));
		this.session = this.cnx.createSession(true, Session.SESSION_TRANSACTED);
		this.dest = this.session.createQueue(qName);
		MessageConsumer consumer = this.session.createConsumer(dest);
		consumer.setMessageListener(this);
	}

	@Override
	public void onMessage(Message msg) {

		// For commits: remember an event can be analyzed multiple times
		// without problems.
		ObjectMessage omsg = (ObjectMessage) msg;
		Event evt;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof Event)) {
				log.warn("An object was received on the event queue but was not an event! Ignored.");
				session.commit();
				return;
			}
			evt = (Event) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during event reception. BAD. Message will stay in queue and will be analysed later",
					e);
			try {
				session.rollback();
			} catch (JMSException e1) {
			}
			return;
		}

		// Analyse event!
		
		try {
			session.commit();
		} catch (JMSException e) {
			log.error(
					"While receiving an application definition, we could not commit reading the message. It is a catastrophy: the db has already been correctly commited with the application data. The scheduler will stop. Empty the APPLICATION queue and restart it.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
		// log.debug(String.format("Application of id %s received", a.getId()));
		log.debug(String.format("Event id %s received", evt.getId()));
	}
}
