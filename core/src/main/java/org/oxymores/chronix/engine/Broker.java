package org.oxymores.chronix.engine;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
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
	private ChronixContext ctx;
	private EntityManagerFactory emf;

	private ActiveMQConnectionFactory factory;
	private Connection connection;

	public Broker(ChronixContext ctx) throws Exception {
		this(ctx, false);
	}

	public Broker(ChronixContext ctx, boolean purge) throws Exception {
		log.info(String
				.format("Starting configuration of a message broker listening on %s (db is %s)",
						ctx.localUrl, ctx.configurationDirectory));
		this.ctx = ctx;
		broker = new BrokerService();
		String brokerName = this.ctx.getBrokerName();
		broker.setBrokerName(brokerName);
		this.emf = Persistence.createEntityManagerFactory("TransacUnit");

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
		broker.addConnector("tcp://" + this.ctx.localUrl);

		// Factory
		this.factory = new ActiveMQConnectionFactory("vm://" + brokerName);

		// Add channels to other nodes
		for (Application a : this.ctx.applicationsById.values()) {
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
		MetadataListener a = new MetadataListener();
		a.startListening(this.connection, brokerName, ctx);

		RunnerAgent r = new RunnerAgent();
		r.startListening(this.connection, brokerName,
				FilenameUtils.concat(ctx.configurationDirectoryPath, "JOBLOG"));

		EventListener e = new EventListener();
		e.startListening(this.connection, brokerName, ctx, emf);

		Pipeline pipe = new Pipeline();
		pipe.startListening(this.connection, brokerName, ctx, emf);

		Runner runner = new Runner();
		runner.startListening(this.connection, brokerName, ctx, emf);

		LogListener ll = new LogListener();
		ll.startListening(this.connection, brokerName, ctx);

		TranscientListener tl = new TranscientListener();
		tl.startListening(this.connection, brokerName, ctx, emf);
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

	public EntityManagerFactory getEmf() {
		return emf;
	}

	public Connection getConnection() {
		return connection;
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
}
