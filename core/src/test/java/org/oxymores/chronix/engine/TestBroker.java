package org.oxymores.chronix.engine;

import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.Event;

// These tests are deprecated.
public class TestBroker {
	private static Logger log = Logger.getLogger(TestBroker.class);

	private ChronixContext ctx1;
	@SuppressWarnings("unused")
	private ChronixContext ctx2;
	private Broker b1;
	@SuppressWarnings("unused")
	private ExecutionNode n1789, n1400;
	private String db1, db2;
	private Application app1;

	@Before
	public void init() throws Exception {
		db1 = "C:\\TEMP\\db1";
		db2 = "C:\\TEMP\\db2";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/
		ChronixContext c1 = new ChronixContext();
		c1.configurationDirectory = new File(db1);
		c1.configurationDirectoryPath = db1;

		// Clear test db directory
		File[] fileList = c1.configurationDirectory.listFiles();
		for (int i = 0; i < fileList.length; i++)
			fileList[i].delete();
		c1.createNewConfigFile();

		// Create test application and save it inside context
		Application a1 = org.oxymores.chronix.demo.DemoApplication.getNewDemoApplication();

		// Init creation of app
		try {
			c1.saveApplication(a1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {
			c1.setWorkingAsCurrent(a1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		/************************************************
		 * Create an empty test configuration db for second node
		 ***********************************************/
		ChronixContext c2 = new ChronixContext();
		c2.configurationDirectory = new File(db2);
		c2.configurationDirectoryPath = db2;

		// Clear test db directory
		File[] fileList2 = c2.configurationDirectory.listFiles();
		for (int i = 0; i < fileList2.length; i++)
			fileList2[i].delete();

		// Write a listener configuration file
		c2.createNewConfigFile(1400, "TransacUnit2", "HistoryUnit2");

		/************************************************
		 * Create test objects from the new db
		 ***********************************************/

		// Load the configuration db into a context
		LogHelpers.clearAllTranscientElements(c1);
		LogHelpers.clearAllTranscientElements(c2);
		ctx1 = ChronixContext.loadContext(db1);
		ctx2 = ChronixContext.loadContext(db2);

		app1 = ctx1.applicationsByName.get(a1.getName());

		// Create a broker from the new context. Purge its queues.
		b1 = new Broker(ctx1, true);

		// Fetch the nodes
		for (ExecutionNode n : ctx1.getNetwork().values()) {
			if (n.getqPort() == 1789)
				n1789 = n;
			else if (n.getqPort() == 1400)
				n1400 = n;
		}
	}

	@After
	public void cleanup() {
		b1.stop();
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
		} // Give it time to really stop. Release locks. Save
			// raccoons. Helps chaining tests.
	}

	@Test
	public void testStart() throws Exception {
		log.info("****This tests sending a text message without anyone listening to it.");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = session.createQueue("TEST.SIMPLESEND");
		MessageProducer producer = session.createProducer(destination);
		String text = "Hello world1! From: " + Thread.currentThread().getName() + " : " + this.hashCode();
		TextMessage message = session.createTextMessage(text);

		producer.send(message);

		session.close();
		connection.close();
	}

	@Test
	public void testPersistence() throws Exception {
		log.info("****This tests sending a text message, killing the broker, restarting it and checking the message is still alive");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("vm://" + ctx1.localUrl.replace(":", "").toUpperCase());

		// Send a message
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		Destination destination = session.createQueue("TEST.PERSIST");
		MessageProducer producer = session.createProducer(destination);
		String text = "Hello world2!" + this.hashCode();
		TextMessage message = session.createTextMessage(text);

		producer.send(message);

		session.close();
		connection.close();

		b1.stop();

		// Restart the broker. To be sure there is no caching, use a new object
		Broker b2 = new Broker(ctx1);

		// Connect to the broker and get the message
		connection = connectionFactory.createConnection();
		connection.start();
		session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		destination = session.createQueue("TEST.PERSIST");
		MessageConsumer consumer = session.createConsumer(destination);
		Message received = consumer.receive();

		TextMessage textMessage = (TextMessage) received;
		String res = textMessage.getText();

		// Cleanup
		consumer.close();
		session.close();
		connection.close();

		// Set broker for the next test
		b1 = b2;

		// Check result
		Assert.assertEquals(res, text);
	}

	@Test
	public void testRunner() throws JMSException, InterruptedException {
		log.info("****This tests running a shell command on the first node");

		// Get relevant data to launch a job
		Chain chain1 = null;
		for (Chain c : app1.getChains()) {
			if (c.getName().equals("chain1")) {
				chain1 = c;
			}
		}
		State s1 = chain1.getStates().get(2);
		Place p1 = s1.getRunsOnPlaces().get(0);

		SenderHelpers.runStateAlone(s1, p1, ctx1);
		Thread.sleep(1000); // Time to consume message

		List<RunLog> res = LogHelpers.displayAllHistory(ctx1);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void testManualLaunchWithConsequences() throws JMSException, InterruptedException {
		log.info("****This tests running a chain on the first node");

		// Get relevant data to launch a job
		Chain chain1 = null;
		for (Chain c : app1.getChains()) {
			if (c.getName().equals("chain1")) {
				chain1 = c;
			}
		}
		State s1 = chain1.getStates().get(0);
		Place p1 = s1.getRunsOnPlaces().get(0);

		SenderHelpers.runStateInsidePlanWithoutCalendarUpdating(s1, p1, ctx1);
		Thread.sleep(1000); // Time to consume message

		List<RunLog> res = LogHelpers.displayAllHistory(ctx1);
		Assert.assertEquals(3, res.size());
	}

	@Test
	public void testCalendarNextOccurrence() throws JMSException, InterruptedException {
		log.info("****This tests creates an event and sends it to a running engine. Analysis should ensue and trigger a +1 in the calendar");
		releaseCalendar();

		// Test calendar updated
		Calendar ca = app1.getCalendars().get(0);
		EntityManager em2 = ctx1.getTransacEM();
		CalendarDay cd = ca.getCurrentOccurrence(em2);
		Assert.assertEquals("02/01/2030", cd.getValue());
	}

	private void releaseCalendar() throws JMSException, InterruptedException {

		// Get relevant data to create the event
		Chain chain4 = null;
		for (Chain c : app1.getChains()) {
			if (c.getName().equals("chain4")) {
				chain4 = c;
			}
		}
		State s1 = chain4.getStates().get(0);
		Place p1 = s1.getRunsOnPlaces().get(0);

		// Create event
		Event e3 = new Event();
		e3.addValue("KEY", "value");
		e3.setApplication(app1);
		e3.setState(s1);
		e3.setPlace(p1);
		e3.setConditionData1(0);
		e3.setLevel0IdU(chain4.getId());
		e3.setLevel1IdU(UUID.randomUUID());

		SenderHelpers.sendEvent(e3, this.ctx1);
		Thread.sleep(3000); // Time to consume message
	}
}
