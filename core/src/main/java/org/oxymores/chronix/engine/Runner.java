package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.Date;
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
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class Runner implements MessageListener {

	private static Logger log = Logger.getLogger(Runner.class);

	private ChronixContext ctx;
	private Session jmsSession;
	private Destination destEndJob, destLogFile, destRequest;
	private Connection jmsConnection;
	private MessageConsumer jmsPipelineConsumer;

	EntityManagerFactory emf;
	EntityManager em;
	EntityTransaction tr;

	private ArrayList<PipelineJob> resolving;

	private MessageProducer producerRunDescription, producerHistory, producerEvents;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		// Save contexts
		this.ctx = ctx;
		this.jmsConnection = cnx;
		this.emf = emf;

		// Internal queue
		resolving = new ArrayList<PipelineJob>();

		// Log
		String qName = String.format("Q.%s.RUNNERMGR", brokerName);
		log.debug(String.format("(%s) Registering a jobrunner listener on queue %s", ctx.configurationDirectoryPath, qName));

		// Create JMS session
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);

		// Register on Request queue
		this.destRequest = this.jmsSession.createQueue(qName);
		this.jmsPipelineConsumer = this.jmsSession.createConsumer(this.destRequest);
		this.jmsPipelineConsumer.setMessageListener(this);

		// Register on End of job queue
		this.destEndJob = this.jmsSession.createQueue(String.format("Q.%s.ENDOFJOB", brokerName));
		this.jmsPipelineConsumer = this.jmsSession.createConsumer(this.destEndJob);
		this.jmsPipelineConsumer.setMessageListener(this);

		// Register on Log Shipping queue
		this.destLogFile = this.jmsSession.createQueue(String.format("Q.%s.LOGFILE", brokerName));
		this.jmsPipelineConsumer = this.jmsSession.createConsumer(this.destLogFile);
		this.jmsPipelineConsumer.setMessageListener(this);

		// Create JPA context for this thread
		this.em = this.emf.createEntityManager();
		this.tr = this.em.getTransaction();

		// Outgoing producer for running commands
		this.producerRunDescription = this.jmsSession.createProducer(null);
		this.producerHistory = this.jmsSession.createProducer(null);
		this.producerEvents = this.jmsSession.createProducer(null);
	}

	public void stopListening() throws JMSException {
		this.jmsPipelineConsumer.close();
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg) {

		if (msg instanceof ObjectMessage) {
			ObjectMessage omsg = (ObjectMessage) msg;
			try {
				Object o = omsg.getObject();
				if ((o instanceof PipelineJob)) {
					PipelineJob pj = (PipelineJob) o;
					log.warn(String.format("Job execution %s request was received", pj.getId()));
					recvPJ(pj);
					commit();
					return;
				}

				if ((o instanceof RunResult)) {
					RunResult rr = (RunResult) o;
					recvRR(rr);
					commit();
					return;
				}

			} catch (JMSException e) {
				log.error("An error occurred during job reception. BAD. Message will stay in queue and will be analysed later", e);
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
				ArrayList<Parameter> prms = resolvedJob.getActive(ctx).getParameters();
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
					this.sendRunDescription(resolvedJob.getRD(ctx), resolvedJob.getPlace(ctx), resolvedJob);
				}

			} catch (JMSException e) {

			}
		}
	}

	private void recvPJ(PipelineJob job) {
		PipelineJob j = em.find(PipelineJob.class, job.getId());
		if (j == null) {
			tr.begin();
			em.persist(job);
			tr.commit();
			j = em.find(PipelineJob.class, job.getId());
		}

		// Check the job is OK
		ActiveNodeBase toRun = null;
		State s = null;
		try {
			toRun = j.getActive(ctx);
			s = j.getState(ctx);
		} catch (Exception e) {
			log.error("A pipeline job was received with no corresponding application data - thrown out");
			return;
		}
		if (s == null) {
			log.error("A pipeline job was received with no corresponding application data - thrown out");
			return;
		}

		tr.begin();
		j.setRunThis(toRun.getCommandName(j, this, ctx));
		resolving.add(j);

		j.setBeganRunningAt(new Date());
		tr.commit();

		if (!toRun.hasPayload()) {
			// No payload - direct to analysis and event throwing
			log.debug(String.format("Job execution request %s corresponds to an element (%s) with only internal execution", j.getId(),
					toRun.getClass()));
			toRun.internalRun(em, ctx, j, this);
			RunResult res = new RunResult();
			res.returnCode = 0;
			res.id1 = j.getId();
			res.end = new Date();
			res.start = res.end;
			res.outOfPlan = j.getOutOfPlan();
			recvRR(res);
		} else if (j.isReady(ctx)) {
			// It has an active part, but no need for dynamic parameters -> just
			// run it (i.e. send it to a runner agent)
			log.debug(String.format(
					"Job execution request %s corresponds to an element with a true execution but no parameters to resolve before run", j.getId()));
			try {
				SenderHelpers.sendHistory(j.getEventLog(ctx), ctx, producerHistory, jmsSession, true);
				this.sendRunDescription(j.getRD(ctx), j.getPlace(ctx), j);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Active part, and dynamic parameters -> resolve parameters.
			// The run will occur at parameter value reception
			log.debug(String.format("Job execution request %s corresponds to an element with a true execution and parameters to resolve before run",
					j.getId()));
			try {
				SenderHelpers.sendHistory(j.getEventLog(ctx), ctx, producerHistory, jmsSession, true);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			toRun.prepareRun(j, this, ctx);
		}
	}

	private void recvRR(RunResult rr) {
		if (rr.outOfPlan) {
			log.info("An out of plan job run has just finished - it won't throw events");
		}
		if (rr.id1 == null)
			return; // Means its a debug job - without PipelineJob (impossible
					// in normal operations)
		log.info(String.format(String.format("Job %s has ended", rr.id1)));

		PipelineJob pj = null;
		for (PipelineJob pj2 : this.resolving) {
			if (pj2.getId().equals(rr.id1)) {
				pj = pj2;
				break;
			}
		}
		if (pj == null) {
			log.error("A result was received that was not waited for - thrown out");
			return;
		}

		State s = null;
		Place p = null;
		Application a = null;
		if (!rr.outOfPlan) {
			s = pj.getState(ctx);
			p = pj.getPlace(ctx);
			a = pj.getApplication(ctx);
			if (s == null) {
				log.error("A result was received for a pipeline job without state - thrown out");
				resolving.remove(pj);
				return;
			}
		}

		// Event throwing
		if (!rr.outOfPlan) {
			Event e = pj.createEvent(rr);
			try {
				SenderHelpers.sendEvent(e, producerEvents, jmsSession, ctx, true);
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// Update the PJ (it will stay in the DB for a while)
		tr.begin();
		pj.setStatus("DONE");
		pj.setBeganRunningAt(rr.start);
		pj.setStoppedRunningAt(rr.end);
		pj.setResultCode(rr.returnCode);
		tr.commit();

		// Send history
		try {
			SenderHelpers.sendHistory(pj.getEventLog(ctx, rr), ctx, producerHistory, jmsSession, true);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Calendar progress
		if (!rr.outOfPlan && s.usesCalendar() && !pj.getIgnoreCalendarUpdating()) {
			Calendar c = a.getCalendar(UUID.fromString(pj.getCalendarID()));
			CalendarDay justDone = c.getDay(UUID.fromString(pj.getCalendarOccurrenceID()));
			CalendarDay next = c.getOccurrenceAfter(justDone);
			CalendarPointer cp = null;
			try {
				cp = s.getCurrentCalendarPointer(em, p);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			tr.begin();
			cp.setLastEndedOccurrenceCd(justDone);
			cp.setRunning(false);
			if (pj.getResultCode() == 0) {
				cp.setLastEndedOkOccurrenceCd(justDone);
				cp.setNextRunOccurrenceCd(next);
			}
			log.debug(String
					.format("At the end of the run, calendar status for state [%s] (chain [%s]) is Last: %s - LastOK: %s - LastStarted: %s - Next: %s - Latest failed: %s - Running: %s",
							s.getRepresents().getName(), s.getChain().getName(), cp.getLastEndedOccurrenceCd(ctx).getValue(), cp
									.getLastEndedOkOccurrenceCd(ctx).getValue(), cp.getLastStartedOccurrenceCd(ctx).getValue(), cp
									.getNextRunOccurrenceCd(ctx).getValue(), cp.getLatestFailed(), cp.getRunning()));
			tr.commit();
		}

		// End
		resolving.remove(pj);
	}

	private void commit() {
		try {
			jmsSession.commit();
		} catch (JMSException e) {
			log.error("failure to acknowledge a message in the JMS queue. Scheduler will now abort as it is a dangerous situation.", e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void rollback() {
		try {
			jmsSession.rollback();
		} catch (JMSException e) {
			log.error("failure to rollback an message in the JMS queue. Scheduler will now abort as it is a dangerous situation.", e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	public void sendRunDescription(RunDescription rd, Place p, PipelineJob pj) throws JMSException {
		// Always send to the node, not its hosting node.
		String qName = String.format("Q.%s.RUNNER", p.getNode().getBrokerName());
		log.info(String.format("A command will be sent for execution on queue %s (%s)", qName, rd.command));
		Destination destination = jmsSession.createQueue(qName);

		ObjectMessage m = jmsSession.createObjectMessage(rd);
		m.setJMSReplyTo(destEndJob);
		m.setJMSCorrelationID(pj.getId());
		producerRunDescription.send(destination, m);
		jmsSession.commit();
	}

	public void sendCalendarPointer(CalendarPointer cp, Calendar ca) throws JMSException {
		SenderHelpers.sendCalendarPointer(cp, ca, jmsSession, this.producerHistory, true);
	}

	public void getParameterValue(RunDescription rd, PipelineJob pj, UUID paramId) throws JMSException {
		// Always send to the node, not its hosting node.
		Place p = pj.getPlace(ctx);
		String qName = String.format("Q.%s.RUNNER", p.getNode().getBrokerName());
		log.info(String.format("A command will be sent for execution on queue %s (%s)", qName, rd.command));
		Destination destination = jmsSession.createQueue(qName);

		ObjectMessage m = jmsSession.createObjectMessage(rd);
		m.setJMSReplyTo(destEndJob);
		m.setJMSCorrelationID(pj.getId() + "|" + paramId);
		producerRunDescription.send(destination, m);
		jmsSession.commit();
	}

	public void sendParameterValue(String value, UUID paramID, PipelineJob pj) throws JMSException {
		// This is a loopback send (used by static parameter value mostly)
		log.debug(String.format("A param value resolved locally (static) will be sent to the local engine ( value is %s)", value));

		TextMessage m = jmsSession.createTextMessage(value);
		m.setJMSCorrelationID(pj.getId() + "|" + paramID.toString());
		producerRunDescription.send(destEndJob, m);
		jmsSession.commit();
	}
}
