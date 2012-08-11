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
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.timedata.RunStats;

public class LogListener implements MessageListener {

	private static Logger log = Logger.getLogger(LogListener.class);

	private Session jmsSession;
	private Destination logQueueDestination;
	private Connection jmsConnection;
	private MessageConsumer jmsLogConsumer;
	private EntityManager emHistory, emTransac;
	private EntityTransaction trHistory, trTransac;
	private ChronixContext ctx;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx) throws JMSException {
		log.debug(String.format("(%s) Initializing LogListener", ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;

		// Register current object as a listener on LOG queue
		String qName = String.format("Q.%s.LOG", brokerName);
		log.debug(String.format("Broker %s: registering a log listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.logQueueDestination = this.jmsSession.createQueue(qName);
		this.jmsLogConsumer = this.jmsSession.createConsumer(logQueueDestination);
		this.jmsLogConsumer.setMessageListener(this);

		// Persistence on two contexts
		this.emHistory = this.ctx.getHistoryEM();
		this.trHistory = this.emHistory.getTransaction();

		this.emTransac = this.ctx.getTransacEM();
		this.trTransac = this.emTransac.getTransaction();
	}

	public void stopListening() throws JMSException {
		this.jmsLogConsumer.close();
		this.jmsSession.close();
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
			log.error("An error occurred during log reception. BAD. Message will stay in queue and will be analysed later", e);
			try {
				jmsSession.rollback();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
			return;
		}
		trHistory.begin();
		trTransac.begin();

		log.info(String.format("An internal log was received. Id: %s - Target: %s - Place: %s - State: %s", rlog.id, rlog.activeNodeName,
				rlog.placeName, rlog.stateId));
		log.debug("\n" + RunLog.getTitle() + "\n" + rlog.getLine());
		emHistory.merge(rlog);
		RunStats.storeMetrics(rlog, emTransac);
		trHistory.commit();
		trTransac.commit();
		try {
			jmsSession.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		trTransac.begin();
		RunStats.updateStats(rlog, emTransac);
		trTransac.commit();

	}
}
