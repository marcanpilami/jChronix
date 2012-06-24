package org.oxymores.chronix.engine;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class Runner implements MessageListener {

	private static Logger log = Logger.getLogger(Runner.class);

	private ChronixContext ctx;
	private Session session;
	private Destination destEndJob, destLogFile, destRequest;
	private Connection cnx;
	EntityManagerFactory emf;
	EntityManager em;
	EntityTransaction tr;

	private MessageProducer producerRunDescription;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		// Save contexts
		this.ctx = ctx;
		this.cnx = cnx;
		this.emf = emf;
		
		// Log
		String qName = String.format("Q.%s.RUNNERMGR", brokerName);
		log.debug(String.format(
				"Broker %s: registering a jobrunner listener on queue %s",
				brokerName, qName));
		
		// Create JMS session
		this.session = this.cnx.createSession(true, Session.SESSION_TRANSACTED);

		// Register on Request queue
		this.destRequest = this.session.createQueue(qName);
		MessageConsumer consumer = this.session.createConsumer(destRequest);
		consumer.setMessageListener(this);

		// Register on End of job queue
		this.destEndJob = this.session.createQueue(String.format("Q.%s.ENDOFJOB", brokerName));
		consumer = this.session.createConsumer(destEndJob);
		consumer.setMessageListener(this);

		// Register on Log Shipping queue
		this.destLogFile = this.session.createQueue(String.format("Q.%s.LOG", brokerName));
		consumer = this.session.createConsumer(destLogFile);
		consumer.setMessageListener(this);

		// Create JPA context for this thread
		this.em = this.emf.createEntityManager();
		this.tr = this.em.getTransaction();

		// Outgoing producer for running commands
		producerRunDescription = session.createProducer(null);
	}

	@Override
	public void onMessage(Message msg) {

		ObjectMessage omsg = (ObjectMessage) msg;
		try {
			Object o = omsg.getObject();
			if ((o instanceof PipelineJob)) {
				PipelineJob pj = (PipelineJob)o;
				log.warn(String.format("Job execution %s request was received", pj.getId()));
				commit();
				recvPJ(pj.getId());
				return;
			}
			
		} catch (JMSException e) {
			log.error(
					"An error occurred during job reception. BAD. Message will stay in queue and will be analysed later",
					e);
			rollback();
			return;
		}
	}
	
	private void recvPJ(String id)
	{
		UUID i = UUID.fromString(id);
		PipelineJob j = em.find(PipelineJob.class, i);
		
		ActiveNodeBase toRun = j.getActive(ctx);
		toRun.run(j, this, ctx, em);
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
	
	
	public void sendRunDescription(RunDescription rd, Place p, PipelineJob pj) throws JMSException
	{
		// Always send to the node, not its hosting node.
		String qName = String.format("Q.%s.RUNNER", p.getNode().getBrokerName());
		log.info(String.format(
				"A command will be sent for execution on queue %s (%s)", qName,
				rd.command));
		Destination destination = session.createQueue(qName);

		ObjectMessage m = session.createObjectMessage(rd);
		m.setJMSReplyTo(destEndJob);
		m.setJMSCorrelationID(pj.getId());
		producerRunDescription.send(destination, m);
		session.commit();
	}
}
