package org.oxymores.chronix.engine;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.NodeLink;

/*
 * Queues are named
 * 
 * Q.<READERDNSPORT> .APPLICATION
 * 				     .EVENT
 * 				     .HISTORY
 * 					 .EXCLREQUEST
 */

public class Broker {
	private static Logger log = Logger.getLogger(Broker.class);

	private BrokerService broker;

	private ActiveMQConnectionFactory factory;
	private Connection connection;
	private MessageProducer producerApp;// , producerEvent;// , producerOrder,
										// producerHistory,
										// producerFile;
	private Session sessionApp;// , sessionEvent;

	public Broker(ChronixContext ctx) throws Exception {
		this(ctx, false);
	}

	public Broker(ChronixContext ctx, boolean purge) throws Exception {
		log.info(String
				.format("Starting configuration of a message broker listening on %s (db is %s)",
						ctx.localUrl, ctx.configurationDirectory));
		broker = new BrokerService();
		String brokerName = ctx.localUrl.replace(":", "").toUpperCase();
		broker.setBrokerName(brokerName);

		// Basic configuration
		broker.setPersistent(true);
		SystemUsage su = new SystemUsage();
		StoreUsage stu = new StoreUsage();
		stu.setLimit(104857600);
		su.setStoreUsage(stu);
		TempUsage tu = new TempUsage();
		tu.setLimit(104857600);
		su.setTempUsage(tu);
		broker.setSystemUsage(su);
		broker.setUseJmx(false); // Not now. Later.

		// Add a listener
		broker.addConnector("tcp://" + ctx.localUrl);

		// Factory
		this.factory = new ActiveMQConnectionFactory("vm://" + brokerName);

		// Add channels to other nodes
		for (Application a : ctx.applicationsById.values()) {
			for (NodeLink nl : a.getLocalNode().getCanSendTo()) {
				if (!nl.getMethod().equals(NodeConnectionMethod.TCP))
					break;

				String url = "static:(tcp://" + nl.getNodeTo().getDns() + ":"
						+ nl.getNodeTo().getqPort() + ")";
				log.info(String
						.format("This broker will be able to open a channel towards %s",
								url));
				NetworkConnector tc = broker.addNetworkConnector(url);
				tc.setDuplex(true);
				tc.setNetworkTTL(10);
			}

			for (NodeLink nl : a.getLocalNode().getCanReceiveFrom()) {
				log.info(String
						.format("This broker should receive channels incoming from %s:%s",
								nl.getNodeFrom().getDns(), nl.getNodeFrom()
										.getqPort()));
			}
		}

		// Purge (for tests, mostly)
		if (purge)
			purgeAllQueues();

		// Start
		log.info("The message broker will now start");
		broker.start();

		// Connect to it...
		this.connection = factory.createConnection();
		this.connection.start();

		// Create & register object listeners
		String qName = String.format("Q.%s.APPLICATION", brokerName);
		log.debug(String.format("Broker %s: registering a listener on queue %s", ctx.configurationDirectory, qName));
		sessionApp = this.connection.createSession(true,
				Session.SESSION_TRANSACTED);
		MetadataListener a = new MetadataListener(sessionApp, ctx);
		Destination dest = sessionApp.createQueue(qName);
		MessageConsumer consumerApp = sessionApp.createConsumer(dest);
		consumerApp.setMessageListener(a);
	}

	public void stop() {
		log.info("The message broker will now stop");
		try {
			broker.stop();
		} catch (Exception e) {
			log.warn(
					"an error occured while trying to stop the broker. Will not impact the scheduler.",
					e);
		} finally {
			try {
				this.connection.stop();
			} catch (JMSException e) {
			}
		}
	}

	public BrokerService getBroker() {
		return this.broker;
	}

	public void purgeAllQueues() throws JMSException {
		log.warn("purge all queues on broker was called");
		try {
			broker.deleteAllMessages();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			log.warn(
					"An error occurend while purging queues. Not a real problem",
					e1);
		}
	}

	public void sendApplication(Application a, ExecutionNode target)
			throws JMSException {
		String qName = String
				.format("Q.%s.APPLICATION", target.getBrokerName());
		log.info(String.format("An app will be sent over the wire on queue %s", qName));

		if (producerApp == null) {
			producerApp = sessionApp.createProducer(null);
		}

		
		Destination destination = sessionApp.createQueue(qName);

		ObjectMessage m = sessionApp.createObjectMessage(a);
		producerApp.send(destination, m);
		sessionApp.commit();
	}
}
