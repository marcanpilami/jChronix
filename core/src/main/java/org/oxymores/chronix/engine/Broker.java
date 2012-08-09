package org.oxymores.chronix.engine;

import java.io.IOException;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.persistence.EntityManagerFactory;

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

	// JMS
	private String brokerName;
	private BrokerService broker;
	private ActiveMQConnectionFactory factory;
	private Connection connection;

	// Chronix running context
	private ChronixContext ctx;
	private ChronixEngine engine;
	private EntityManagerFactory emf;

	// Threads
	MetadataListener thrML;
	RunnerAgent thrRA;
	EventListener thrEL;
	Pipeline thrPL;
	Runner thrRU;
	LogListener thrLL;
	TranscientListener thrTL;

	public Broker(ChronixEngine engine) throws Exception {
		this(engine, false);
	}

	public Broker(ChronixContext ctx) throws Exception {
		this(ctx, false);
	}

	public Broker(ChronixEngine engine, boolean purge) throws Exception {
		this(engine.ctx, purge);
	}

	public Broker(ChronixContext ctx, boolean purge) throws Exception {
		log.info(String.format("Starting configuration of a message broker listening on %s (db is %s)", ctx.localUrl, ctx.configurationDirectory));
		this.ctx = ctx;
		broker = new BrokerService();
		brokerName = this.ctx.getBrokerName();
		broker.setBrokerName(brokerName);
		this.emf = ctx.getTransacEMF();

		// Basic configuration
		broker.setPersistent(true);
		//broker.setBrokerId(UUID.randomUUID().toString());
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

		// Add channels to other nodes
		for (Application a : this.ctx.applicationsById.values()) {
			for (NodeLink nl : a.getLocalNode().getCanSendTo()) {
				if (!nl.getMethod().equals(NodeConnectionMethod.TCP))
					break;

				String url = "static:(tcp://" + nl.getNodeTo().getDns() + ":" + nl.getNodeTo().getqPort() + ")";
				log.info(String.format("This broker will be able to open a channel towards %s", url));
				NetworkConnector tc = broker.addNetworkConnector(url);
				tc.setDuplex(true);
				tc.setNetworkTTL(20);
			}

			for (NodeLink nl : a.getLocalNode().getCanReceiveFrom()) {
				log.info(String.format("This broker should receive channels incoming from %s:%s", nl.getNodeFrom().getDns(), nl.getNodeFrom()
						.getqPort()));
			}
		}

		// Purge (for tests, mostly)
		if (purge)
			purgeAllQueues();

		// Start
		log.info("The message broker will now start");
		broker.start();

		// Factory
		this.factory = new ActiveMQConnectionFactory("vm://" + brokerName);

		// Connect to it...
		this.connection = factory.createConnection();
		this.connection.start();
	}

	public void registerListeners(ChronixEngine engine) throws JMSException, IOException {
		registerListeners(engine, true, true, true, true, true, true);
	}

	public void registerListeners(ChronixEngine engine, boolean startMeta, boolean startRunnerAgnet, boolean startPipeline, boolean startRunner,
			boolean startLog, boolean startTranscient) throws JMSException, IOException {
		this.engine = engine;

		this.thrML = new MetadataListener();
		this.thrML.startListening(this.connection, brokerName, ctx, this.engine);

		this.thrRA = new RunnerAgent();
		this.thrRA.startListening(this.connection, brokerName, FilenameUtils.concat(ctx.configurationDirectoryPath, "JOBLOG"));

		this.thrEL = new EventListener();
		this.thrEL.startListening(this.connection, brokerName, ctx, emf);

		this.thrPL = new Pipeline();
		this.thrPL.startListening(this.connection, brokerName, ctx, emf);

		this.thrRU = new Runner();
		this.thrRU.startListening(this.connection, brokerName, ctx, emf);

		this.thrLL = new LogListener();
		this.thrLL.startListening(this.connection, brokerName, ctx);

		this.thrTL = new TranscientListener();
		this.thrTL.startListening(this.connection, brokerName, ctx, emf);
	}

	public void stop() {
		log.info(String.format("(%s) The message broker will now stop", this.ctx.configurationDirectoryPath));
		try {
			if (this.thrML != null)
				this.thrML.stopListening();
			if (this.thrRA != null)
				this.thrRA.stopListening();
			if (this.thrEL != null)
				this.thrEL.stopListening();
			if (this.thrPL != null)
				this.thrPL.stopListening();
			if (this.thrRU != null)
				this.thrRU.stopListening();
			if (this.thrLL != null)
				this.thrLL.stopListening();
			if (this.thrTL != null)
				this.thrTL.stopListening();

			for (NetworkConnector nc : this.broker.getNetworkConnectors()) {
				// nc.stop();
				this.broker.removeNetworkConnector(nc);
			}

			this.connection.close();
			broker.stop();
		} catch (Exception e) {
			log.warn("an error occured while trying to stop the broker. Will not impact the scheduler.", e);
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
			log.warn("An error occurend while purging queues. Not a real problem", e1);
		}
	}
}
