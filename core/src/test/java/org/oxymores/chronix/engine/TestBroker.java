package org.oxymores.chronix.engine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.Event;

public class TestBroker {
	private static Logger log = Logger.getLogger(TestBroker.class);

	private ChronixContext ctx1;
	private ChronixContext ctx2;
	private Broker b1;
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

		int i = ctx2.configurationDirectory.listFiles().length;
		Assert.assertEquals(2, i);
	}

	@Test
	public void testRunner() throws JMSException, InterruptedException {
		log.info("****This tests running a shell command on the first node");
		b1.sendCommand("echo aa", n1789);
		Thread.sleep(2000); // Time to consume message
	}

	@Test
	public void testEventListener() throws JMSException, InterruptedException {
		log.info("****This tests creates an event and sends it to a running engine. Analysis should ensue.");

		// Get relevant data to create the event
		Chain chain1 = null;
		for (Chain c : app1.getChains()) {
			if (c.getName().equals("chain1")) {
				chain1 = c;
			}
		}
		State s1 = chain1.getStates().get(0);
		Place p1 = s1.getRunsOnPlaces().get(0);

		// Create event
		Event e1 = new Event();
		e1.addValue("KEY", "value");
		e1.setApplication(app1);
		e1.setState(s1);
		e1.setPlace(p1);
		e1.setConditionData1(0);
		e1.setLevel0IdU(chain1.getId());

		b1.sendEvent(e1);
		Thread.sleep(5000); // Time to consume message

		// tests
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("HistoryUnit");
		EntityManager em = emf.createEntityManager();
		Query q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
		@SuppressWarnings("unchecked")
		List<RunLog> res = q.getResultList();
		Assert.assertEquals(2, res.size());

		log.info(RunLog.getTitle());
		for (RunLog l : res) {
			log.info(l.getLine());
		}
	}
}
