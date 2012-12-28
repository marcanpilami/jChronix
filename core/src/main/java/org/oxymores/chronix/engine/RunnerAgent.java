package org.oxymores.chronix.engine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class RunnerAgent implements MessageListener {
	private static Logger log = Logger.getLogger(RunnerAgent.class);

	private Session jmsSession;
	private Destination jmsRunnerDestination;
	private Connection jmsConnection;
	private MessageProducer jmsProducer;
	private MessageConsumer jmsRequestConsumer;

	private String logDbPath;

	public void startListening(Connection cnx, String brokerName, String logDbPath) throws JMSException, IOException {
		log.info(String.format("(%s) Starting a runner agent", brokerName));

		// Pointers
		this.jmsConnection = cnx;

		// Log repository
		this.logDbPath = FilenameUtils.normalize(logDbPath);
		if (!(new File(this.logDbPath)).exists()) {
			(new File(this.logDbPath)).mkdir();
		}

		// Queue listener
		String qName = String.format("Q.%s.RUNNER", brokerName);
		log.debug(String.format("Broker %s: registering a runner listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsRunnerDestination = this.jmsSession.createQueue(qName);
		this.jmsRequestConsumer = this.jmsSession.createConsumer(this.jmsRunnerDestination);

		// Producer to send run results
		this.jmsProducer = this.jmsSession.createProducer(null);

		// start listening
		this.jmsRequestConsumer.setMessageListener(this);
	}

	public void stopListening() throws JMSException {
		this.jmsProducer.close();
		this.jmsRequestConsumer.close();
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg) {
		log.info("Run request received");
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
			log.error("An error occurred during RunDescription reception. BAD. Message will stay in queue and will be analysed later", e);
			rollback();
			return;
		}

		if (!rd.helperExecRequest)
			log.info(String.format("Running command %s", rd.command));
		else
			log.debug(String.format("Running helper internal command %s", rd.command));

		// Log file (only if true run)
		String logFilePath = null;
		String logFileName = null;
		Date start = new Date();
		if (!rd.helperExecRequest) {

			SimpleDateFormat myFormatDir = new SimpleDateFormat("yyyyMMdd");
			SimpleDateFormat myFormatFile = new SimpleDateFormat("yyyyMMddhhmmssSSS");
			String dd = myFormatDir.format(start);
			String logFileDateDir = FilenameUtils.concat(this.logDbPath, dd);
			if (!(new File(logFileDateDir)).exists()) {
				(new File(logFileDateDir)).mkdir();
			}
			logFileName = String.format("%s_%s_%s_%s.log", myFormatFile.format(start), rd.placeName.replace(" ", "-"),
					rd.activeSourceName.replace(" ", "-"), rd.id1);
			logFilePath = FilenameUtils.concat(logFileDateDir, logFileName);
		}

		// Run the command according to its method
		RunResult res = null;

		if (rd.Method.equals("Shell"))
			res = RunnerShell.run(rd, logFilePath, !rd.helperExecRequest, rd.shouldSendLogFile);
		else {
			res = new RunResult();
			res.returnCode = -1;
			res.logStart = String.format("An unimplemented exec method (%s) was called!", rd.Method);
			log.error(String.format("An unimplemented exec method (%s) was called!", rd.Method));
		}
		res.start = start;
		res.end = new Date();

		// Copy the engine ids - that way it will be able to identify the launch
		// Part of the ids are in the JMS correlation id too
		res.id1 = rd.id1;
		res.id2 = rd.id2;
		res.outOfPlan = rd.outOfPlan;

		// Send the result!
		Message response;
		if (!rd.helperExecRequest) {
			try {
				response = jmsSession.createObjectMessage(res);
				response.setJMSCorrelationID(msg.getJMSCorrelationID());
				jmsProducer.send(msg.getJMSReplyTo(), response);
				
				if (res.logSizeBytes <= 500000)
				{
					response = jmsSession.createBytesMessage();
					byte[] bytes = new byte[(int) res.logSizeBytes];
					InputStream is = new FileInputStream(res.logPath);
					is.read(bytes);
					is.close();
					
					((BytesMessage) response).writeBytes(bytes);
					response.setJMSCorrelationID(msg.getJMSCorrelationID());
					response.setStringProperty("FileName", logFileName);
					jmsProducer.send(msg.getJMSReplyTo(), response);
				}
				else
					log.warn("A log file was too big and will not be sent. Only the full log file will be missing - the launch will still appear in the console.");
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				response = jmsSession.createTextMessage(res.logStart);
				response.setJMSCorrelationID(msg.getJMSCorrelationID());
				jmsProducer.send(msg.getJMSReplyTo(), response);
			} catch (JMSException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		try {
			jmsSession.commit();
		} catch (JMSException e) {
			log.error("oups", e);
			System.exit(1);
		}
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

}
