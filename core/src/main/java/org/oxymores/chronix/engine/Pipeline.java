package org.oxymores.chronix.engine;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class Pipeline extends Thread implements MessageListener {

	private static Logger log = Logger.getLogger(Pipeline.class);

	private ChronixContext ctx;
	private Session session;
	private Destination dest;
	private Connection cnx;
	EntityManagerFactory emf;
	EntityManager entityManager, em2;
	EntityTransaction transaction, tr2;

	private MessageProducer producerJR;

	private LinkedBlockingQueue<PipelineJob> entering;
	private ConcurrentLinkedQueue<PipelineJob> waiting_calendar;
	private ConcurrentLinkedQueue<PipelineJob> waiting_sequence;
	private ConcurrentLinkedQueue<PipelineJob> waiting_token;
	private ConcurrentLinkedQueue<PipelineJob> waiting_exclusion;
	private ConcurrentLinkedQueue<PipelineJob> waiting_run;

	private Boolean run = true;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		this.ctx = ctx;
		this.cnx = cnx;
		String qName = String.format("Q.%s.PJ", brokerName);
		log.debug(String.format(
				"Broker %s: registering a pipeline listener on queue %s",
				brokerName, qName));
		this.session = this.cnx.createSession(true, Session.SESSION_TRANSACTED);
		this.dest = this.session.createQueue(qName);
		MessageConsumer consumer = this.session.createConsumer(dest);
		consumer.setMessageListener(this);

		this.emf = emf;
		entityManager = emf.createEntityManager();
		transaction = entityManager.getTransaction();

		em2 = emf.createEntityManager();
		tr2 = em2.getTransaction();

		qName = String.format("Q.%s.PJ", brokerName);
		producerJR = session.createProducer(null);

		// Create analysis queues
		entering = new LinkedBlockingQueue<PipelineJob>();
		waiting_calendar = new ConcurrentLinkedQueue<PipelineJob>();
		waiting_sequence = new ConcurrentLinkedQueue<PipelineJob>();
		waiting_token = new ConcurrentLinkedQueue<PipelineJob>();
		waiting_exclusion = new ConcurrentLinkedQueue<PipelineJob>();
		waiting_run = new ConcurrentLinkedQueue<PipelineJob>();

		// Retrieve jobs from previous service launches
		Query q = entityManager
				.createQuery("SELECT j FROM PipelineJob j WHERE j.status = ?1");
		q.setParameter(1, "CHECK_SYNC_CONDS");
		@SuppressWarnings("unchecked")
		List<PipelineJob> sessionEvents = q.getResultList();
		waiting_calendar.addAll(sessionEvents);

		// Start thread
		this.start();
	}

	@Override
	public void run() {
		while (run) {
			PipelineJob pj = null;
			try {
				pj = entering.poll(1, TimeUnit.MINUTES);
			} catch (InterruptedException e) {
				// Interruption is all right.
			}

			if (pj != null)
				waiting_calendar.add(pj);

			for (PipelineJob j : waiting_calendar) {
				anCalendar(j);
			}

			for (PipelineJob j : waiting_sequence) {
				anSequence(j);
			}

			for (PipelineJob j : waiting_token) {
				anToken(j);
			}

			for (PipelineJob j : waiting_exclusion) {
				anExclusion(j);
			}

			for (PipelineJob j : waiting_run) {
				run(j);
			}
		}
	}

	@Override
	public void onMessage(Message msg) {
		ObjectMessage omsg = (ObjectMessage) msg;
		PipelineJob pj;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof PipelineJob)) {
				log.warn("An object was received on the pipeline queue but was not a job! Ignored.");
				commit();
				return;
			}
			pj = (PipelineJob) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during job reception. BAD. Message will stay in queue and will be analysed later",
					e);
			rollback();
			return;
		}

		tr2.begin();
		pj.setStatus("CHECK_SYNC_CONDS"); // So that we find it again after a
											// crash/stop
		em2.persist(pj);
		tr2.commit();

		commit();

		log.debug("A job has entered the pipeline");
		entering.add(pj);
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

	private void anCalendar(PipelineJob pj) {
		// TODO: really check calendar
		waiting_calendar.remove(pj);
		waiting_sequence.add(pj);
		log.debug(String
				.format("Job %s has finished calendar analysis - on to sequence analysis",
						pj.getId()));
	}

	private void anSequence(PipelineJob pj) {
		// TODO: really check calendar
		waiting_sequence.remove(pj);
		waiting_token.add(pj);
		log.debug(String.format(
				"Job %s has finished sequence analysis - on to token analysis",
				pj.getId()));
	}

	private void anToken(PipelineJob pj) {
		// TODO: really check calendar
		waiting_token.remove(pj);
		waiting_exclusion.add(pj);
		log.debug(String
				.format("Job %s has finished token analysis - on to exclusion analysis",
						pj.getId()));
	}

	private void anExclusion(PipelineJob pj) {
		// TODO: really check calendar
		waiting_exclusion.remove(pj);
		waiting_run.add(pj);
		log.debug(String.format(
				"Job %s has finished exclusion analysis - on to run",
				pj.getId()));
	}

	private void run(PipelineJob pj) {
		// Remove from queue
		waiting_run.remove(pj);

		transaction.begin(); // JPA transaction
		
		String qName = String.format("Q.%s.RUNNERMGR", pj.getPlace(ctx)
				.getNode().getBrokerName());
		try {
			Destination d = session.createQueue(qName);
			ObjectMessage om = session.createObjectMessage(pj);
			producerJR.send(d, om);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//entityManager.merge(pj);
		pj.setStatus("RUNNING");

		transaction.commit(); // JPA
		commit(); // JMS

		log.debug(String.format("Job %s was given to the runner queue",
				pj.getId()));
	}
}
