package org.oxymores.chronix.engine;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;

public class LogListener implements MessageListener {

	private static Logger log = Logger.getLogger(LogListener.class);

	private Session jmsSession;
	private Destination logQueueDestination;
	private Connection jmsConnection;
	private EntityManager em;
	private EntityTransaction tr;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx) throws JMSException {
		log.debug(String.format("Initializing LogListener on context %s",
				ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;

		// Register current object as a listener on LOG queue
		String qName = String.format("Q.%s.LOG", brokerName);
		log.debug(String.format(
				"Broker %s: registering a log listener on queue %s",
				brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true,
				Session.SESSION_TRANSACTED);
		this.logQueueDestination = this.jmsSession.createQueue(qName);
		MessageConsumer consumer = this.jmsSession
				.createConsumer(logQueueDestination);
		consumer.setMessageListener(this);

		// Persistence on dedicated context
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("HistoryUnit");
		em = emf.createEntityManager();
		tr = em.getTransaction();
	}

	@Override
	public void onMessage(Message msg) {
		ObjectMessage omsg = (ObjectMessage) msg;
		RunLog rlog;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof RunLog)) {
				log.warn("An object was received on the log queue but was not a log! Ignored.");
				jmsSession.commit();
				return;
			}
			rlog = (RunLog) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during event reception. BAD. Message will stay in queue and will be analysed later",
					e);
			try {
				jmsSession.rollback();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
			return;
		}
		tr.begin();

		log.info(String
				.format("An internal log was received. Id: %s - Target: %s - Place: %s - State: %s",
						rlog.id, rlog.activeNodeName, rlog.placeName,
						rlog.stateId));
		em.merge(rlog);
		tr.commit();
		try {
			jmsSession.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
