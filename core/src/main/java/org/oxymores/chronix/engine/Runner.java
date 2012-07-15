package org.oxymores.chronix.engine;

import java.util.ArrayList;
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
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class Runner implements MessageListener {

	private static Logger log = Logger.getLogger(Runner.class);

	private Broker broker;
	private ChronixContext ctx;
	private Session session;
	private Destination destEndJob, destLogFile, destRequest;
	private Connection cnx;
	EntityManagerFactory emf;
	EntityManager em;
	EntityTransaction tr;

	private ArrayList<PipelineJob> resolving;

	private MessageProducer producerRunDescription;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf, Broker br)
			throws JMSException {
		// Save contexts
		this.ctx = ctx;
		this.cnx = cnx;
		this.emf = emf;
		this.broker = br;

		// Internal queue
		resolving = new ArrayList<PipelineJob>();

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
		this.destEndJob = this.session.createQueue(String.format(
				"Q.%s.ENDOFJOB", brokerName));
		consumer = this.session.createConsumer(destEndJob);
		consumer.setMessageListener(this);

		// Register on Log Shipping queue
		this.destLogFile = this.session.createQueue(String.format("Q.%s.LOG",
				brokerName));
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

		if (msg instanceof ObjectMessage) {
			ObjectMessage omsg = (ObjectMessage) msg;
			try {
				Object o = omsg.getObject();
				if ((o instanceof PipelineJob)) {
					PipelineJob pj = (PipelineJob) o;
					log.warn(String.format(
							"Job execution %s request was received", pj.getId()));
					commit();
					recvPJ(pj);
					return;
				}

				if ((o instanceof RunResult)) {
					RunResult rr = (RunResult) o;
					recvRR(rr);
					commit();
					return;
				}

			} catch (JMSException e) {
				log.error(
						"An error occurred during job reception. BAD. Message will stay in queue and will be analysed later",
						e);
				rollback();
				return;
			}
		} else if (msg instanceof TextMessage) {
			TextMessage tmsg = (TextMessage) msg;

			try {
				String res = tmsg.getText();
				String cid = msg.getJMSCorrelationID();

				String pjid = cid.split("\\|")[0];
				String paramid = cid.split("\\|")[1];

				PipelineJob resolvedJob = null;
				for (PipelineJob pj : this.resolving) {
					if (pj.getId().toString().equals(pjid)) {
						resolvedJob = pj;
						break;
					}
				}

				int paramIndex = -1;
				ArrayList<Parameter> prms = resolvedJob.getActive(ctx)
						.getParameters();
				for (int i = 0; i < prms.size(); i++) {
					if (prms.get(i).getId().toString().equals(paramid)) {
						paramIndex = i;
						break;
					}
				}

				if (paramIndex == -1 || resolvedJob == null) {
					log.error("received a param resolution for a job that is not in queue - ignored");
					return;
				}

				tr.begin();
				resolvedJob.setParamValue(paramIndex, res);
				tr.commit();

				if (resolvedJob.isReady(ctx)) {
					this.sendRunDescription(resolvedJob.getRD(ctx),
							resolvedJob.getPlace(ctx), resolvedJob);
				}

			} catch (JMSException e) {

			}
		}
	}

	private void recvPJ(PipelineJob job) {
		if (job.getOutOfPlan()) {
			// out of plan jobs are not yet persisted at this time
			// (they are given fresh and raw to this thread)
			tr.begin();
			em.persist(job);
			tr.commit();
		}

		recvPJ(job.getId());
	}

	private void recvPJ(String id) {
		UUID i = UUID.fromString(id);
		PipelineJob j = em.find(PipelineJob.class, i);
		j.setRunThis(j.getActive(ctx).getCommandName(j, this, ctx));
		resolving.add(j);

		ActiveNodeBase toRun = j.getActive(ctx);

		if (! toRun.hasPayload())
		{
			// No payload - direct to analysis and event throwing
			RunResult res = new RunResult();
			res.returnCode = 0;
			res.id1 = j.getId();
			recvRR(res);
		}
		else if (j.isReady(ctx)) {
			try {
				this.sendRunDescription(j.getRD(ctx), j.getPlace(ctx), j);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else
			toRun.prepareRun(j, this, ctx);
	}

	private void recvRR(RunResult rr) {
		if (rr.outOfPlan) {
			log.info("An out of plan job run has just finished - it won't be analysed");
			return;
		}
		log.info(String.format(String.format("Job %s has ended", rr.id1)));
		
		PipelineJob pj = null;
		for (PipelineJob pj2 : this.resolving) {
			if (pj2.getId().equals(rr.id1)) {
				pj = pj2;
				break;
			}
		}

		// Event throwing
		Event e = pj.createEvent(rr);
		try {
			broker.sendEvent(e);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Update the PJ (it will stay in the DB for a while)
		tr.begin();
		pj.setStatus("DONE");
		tr.commit();

		// TODO: advance calendars...
		
		// End
		resolving.remove(pj);
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

	public void sendRunDescription(RunDescription rd, Place p, PipelineJob pj)
			throws JMSException {
		// Always send to the node, not its hosting node.
		String qName = String
				.format("Q.%s.RUNNER", p.getNode().getBrokerName());
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

	public void getParameterValue(RunDescription rd, PipelineJob pj,
			UUID paramId) throws JMSException {
		// Always send to the node, not its hosting node.
		Place p = pj.getPlace(ctx);
		String qName = String
				.format("Q.%s.RUNNER", p.getNode().getBrokerName());
		log.info(String.format(
				"A command will be sent for execution on queue %s (%s)", qName,
				rd.command));
		Destination destination = session.createQueue(qName);

		ObjectMessage m = session.createObjectMessage(rd);
		m.setJMSReplyTo(destEndJob);
		m.setJMSCorrelationID(pj.getId() + "|" + paramId);
		producerRunDescription.send(destination, m);
		session.commit();
	}

	public void sendParameterValue(String value, UUID paramID, PipelineJob pj)
			throws JMSException {
		// This is a loopback send (used by static parameter value mostly)
		log.debug(String
				.format("A param value resolved locally (static) will be sent to the local engine ( value is %s)",
						value));

		TextMessage m = session.createTextMessage(value);
		m.setJMSCorrelationID(pj.getId() + "|" + paramID.toString());
		producerRunDescription.send(destEndJob, m);
		session.commit();
	}
}
