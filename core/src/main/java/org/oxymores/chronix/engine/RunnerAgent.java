package org.oxymores.chronix.engine;

import java.util.Date;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.log4j.Logger;

public class RunnerAgent implements MessageListener {

	private static Logger log = Logger.getLogger(RunnerAgent.class);
	private Session session;
	private Destination dest;
	private Connection cnx;
	private MessageProducer producer;

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

		producer = session.createProducer(null);
	}

	@Override
	public void onMessage(Message msg) {
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

		if (!rd.helperExecRequest)
			log.info(String.format("Running command %s", rd.command));
		else
			log.debug(String.format("Running helper internal command %s",
					rd.command));

		// Run the command according to its method
		RunResult res = null;
		Date start = new Date();
		if (rd.Method.equals("Shell"))
			res = RunnerShell.run(rd);
		else {
			res = new RunResult();
			res.returnCode = -1;
			res.logStart = String.format(
					"An unimplemented exec method (%s) was called!", rd.Method);
			log.error(String.format(
					"An unimplemented exec method (%s) was called!", rd.Method));
		}
		res.start = start;
		res.end = new Date();

		// Copy the engine ids - that way it will be able to identify the launch
		// Part of the ids are in the JMS correlation id too
		res.id1 = rd.id1;
		res.id2 = rd.id2;
		res.outOfPlan = rd.outOfPlan;

		// Env var analysis is done here, so as to enable the remote engine to
		// know them without waiting for the log file
		// TODO: env var analysis

		// Send the result!
		Message response;
		if (!rd.helperExecRequest) {
			try {
				response = session.createObjectMessage(res);
				response.setJMSCorrelationID(msg.getJMSCorrelationID());
				producer.send(msg.getJMSReplyTo(), response);
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			try {
				response = session.createTextMessage(res.logStart);
				response.setJMSCorrelationID(msg.getJMSCorrelationID());
				producer.send(msg.getJMSReplyTo(), response);
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		try {
			session.commit();
		} catch (JMSException e) {
			log.error("oups", e);
			System.exit(1);
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
