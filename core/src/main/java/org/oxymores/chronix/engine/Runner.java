package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
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

	private ArrayList<PipelineJob> resolving;

	private MessageProducer producerRunDescription, producerHistory,
			producerEvents;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		// Save contexts
		this.ctx = ctx;
		this.cnx = cnx;
		this.emf = emf;

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
		this.destLogFile = this.session.createQueue(String.format(
				"Q.%s.LOGFILE", brokerName));
		consumer = this.session.createConsumer(destLogFile);
		consumer.setMessageListener(this);

		// Create JPA context for this thread
		this.em = this.emf.createEntityManager();
		this.tr = this.em.getTransaction();

		// Outgoing producer for running commands
		producerRunDescription = session.createProducer(null);
		producerHistory = session.createProducer(null);
		producerEvents = session.createProducer(null);
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
		j.setBeganRunningAt(new Date());

		if (!toRun.hasPayload()) {
			// No payload - direct to analysis and event throwing
			log.debug(String
					.format("Job execution request %s corresponds to an element (%s) without true execution",
							j.getId(), toRun.getClass()));
			toRun.internalRun(em, ctx, j, this);
			RunResult res = new RunResult();
			res.returnCode = 0;
			res.id1 = j.getId();
			res.end = new Date();
			res.start = res.end;
			recvRR(res);
		} else if (j.isReady(ctx)) {
			// It has an active part, but no need for dynamic parameters -> just
			// run it (i.e. send it to a runner agent)
			log.debug(String
					.format("Job execution request %s corresponds to an element with a true execution but no parameters to resolve before run",
							j.getId()));
			try {
				this.sendHistory(j.getEventLog(ctx));
				this.sendRunDescription(j.getRD(ctx), j.getPlace(ctx), j);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// Active part, and dynamic parameters -> resolve parameters.
			// The run will occur at parameter value reception
			log.debug(String
					.format("Job execution request %s corresponds to an element with a true execution and parameters to resolve before run",
							j.getId()));
			try {
				this.sendHistory(j.getEventLog(ctx));
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			toRun.prepareRun(j, this, ctx);
		}
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
		State s = pj.getState(ctx);
		Place p = pj.getPlace(ctx);
		Application a = pj.getApplication(ctx);

		// Event throwing
		Event e = pj.createEvent(rr);
		try {
			SenderHelpers.sendEvent(e, producerEvents, session, ctx, true);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
			this.sendHistory(pj.getEventLog(ctx));
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Calendar progress
		if (s.usesCalendar() && !pj.getIgnoreCalendarUpdating()) {
			log.debug(pj.getCalendarID());
			Calendar c = a.getCalendar(UUID.fromString(pj.getCalendarID()));
			CalendarDay justDone = c.getDay(UUID.fromString(pj
					.getCalendarOccurrenceID()));
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
							s.getRepresents().getName(),
							s.getChain().getName(), cp
									.getLastEndedOccurrenceCd(ctx).getValue(),
							cp.getLastEndedOkOccurrenceCd(ctx).getValue(),
							cp.getLastStartedOccurrenceCd(ctx).getValue(), cp
									.getNextRunOccurrenceCd(ctx).getValue(), cp
									.getLatestFailed(), cp.getRunning()));
			tr.commit();
		}

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

	public void sendHistory(RunLog rl) throws JMSException {
		// Always send both to ourselves and to the supervisor
		Application a = ctx.applicationsById.get(UUID
				.fromString(rl.applicationId));
		ExecutionNode self = a.getLocalNode();

		String qName = String.format("Q.%s.LOG", self.getBrokerName());
		log.info(String.format("A scheduler log will be sent on queue %s (%s)",
				qName, rl.activeNodeName));
		Destination destination = session.createQueue(qName);

		ObjectMessage m = session.createObjectMessage(rl);
		producerHistory.send(destination, m);
		session.commit();
	}

	public void sendCalendarPointer(CalendarPointer cp, Calendar ca)
			throws JMSException {
		// Send the updated CP to other execution nodes that may need it.
		List<State> states_using_calendar = ca.getUsedInStates();
		List<ExecutionNode> en_using_calendar = new ArrayList<ExecutionNode>();
		ExecutionNode tmp = null;
		for (State s : states_using_calendar) {
			for (Place p : s.getRunsOn().getPlaces()) {
				tmp = p.getNode().getHost();
				if (!en_using_calendar.contains(tmp))
					en_using_calendar.add(tmp);
			}
		}
		// TODO: add supervisor to the list (always)
		log.debug(String
				.format("The pointer should be sent to %s execution nodes (for %s possible customer state(s))",
						en_using_calendar.size(), states_using_calendar.size()));

		// Create message
		ObjectMessage m = session.createObjectMessage(cp);

		// Send the message to every client execution node
		for (ExecutionNode en : en_using_calendar) {
			String qName = String.format("Q.%s.CALENDARPOINTER",
					en.getBrokerName());
			log.info(String.format(
					"A calendar pointer will be sent on queue %s", qName));
			Destination destination = session.createQueue(qName);
			producerHistory.send(destination, m);
		}

		// Send
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
