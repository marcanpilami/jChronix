package org.oxymores.chronix.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.InetAddress;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;

import junit.framework.Assert;

public class TestBroker {
	private static Logger log = Logger.getLogger(TestBroker.class);

	private ChronixContext ctx1;
	private ChronixContext ctx2;
	private Broker b1;
	private ExecutionNode n1789, n1400;

	@Before
	public void init() throws Exception {
		String db1 = "C:\\TEMP\\db1";
		String db2 = "C:\\TEMP\\db2";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/
		ChronixContext c1 = new ChronixContext();
		c1.configurationDirectory = new File(db1);

		// Clear test db directory
		File[] fileList = c1.configurationDirectory.listFiles();
		for (int i = 0; i < fileList.length; i++)
			fileList[i].delete();

		// Create test application and save it inside context
		Application a1 = org.oxymores.chronix.demo.DemoApplication
				.getNewDemoApplication();

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

		// Clear test db directory
		File[] fileList2 = c2.configurationDirectory.listFiles();
		for (int i = 0; i < fileList2.length; i++)
			fileList2[i].delete();

		// Write a listener configuration file
		File f = new File(c2.configurationDirectory + "/listener.crn");
		Writer output = new BufferedWriter(new FileWriter(f));
		String url = InetAddress.getLocalHost().getCanonicalHostName()
				+ ":1400";
		output.write(url);
		output.close();

		/************************************************
		 * Create test objects from the new db
		 ***********************************************/

		// Load the configuration db into a context
		ctx1 = ChronixContext.loadContext(db1);
		ctx2 = ChronixContext.loadContext(db2);

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
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"vm://localhost");
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);
		Destination destination = session.createQueue("TEST.SIMPLESEND");
		MessageProducer producer = session.createProducer(destination);
		String text = "Hello world1! From: " + Thread.currentThread().getName()
				+ " : " + this.hashCode();
		TextMessage message = session.createTextMessage(text);

		producer.send(message);

		session.close();
		connection.close();
	}

	@Test
	public void testPersistence() throws Exception {
		log.info("****This tests sending a text message, killing the broker, restarting it and checking the message is still alive");
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
				"vm://" + ctx1.localUrl.replace(":", "").toUpperCase());

		// Send a message
		Connection connection = connectionFactory.createConnection();
		connection.start();
		Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);
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
	public void testSendApplication() throws JMSException {
		log.info("****This tests sending, an app without an active engine nor anything to receive it on the other side");
		Application a = ctx1.applicationsById.values().iterator().next();
		b1.sendApplication(a, n1400);
	}

	@Test
	public void testReceiveApplication() throws Exception {
		log.info("****This tests sending, receving, parsing and saving of an app without an active engine");
		Application a = ctx1.applicationsById.values().iterator().next(); // who
																			// cares
																			// what
																			// we
																			// send
		Broker b2 = new Broker(ctx2, true);
		b1.sendApplication(a, n1400);
		Thread.sleep(2000); // Time to consume message
		b2.stop();
	}
	
	@Test
	public void testRunner() throws JMSException, InterruptedException {
		log.info("****This tests running a shell command on the first node");
		b1.sendCommand("echo aa", n1789);
		Thread.sleep(2000); // Time to consume message
	}
}
