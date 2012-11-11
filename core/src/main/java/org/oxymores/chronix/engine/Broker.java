package org.oxymores.chronix.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.persistence.EntityManagerFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.usage.MemoryUsage;
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
	ArrayList<RunnerAgent> thrsRA;
	EventListener thrEL;
	Pipeline thrPL;
	Runner thrRU;
	LogListener thrLL;
	TranscientListener thrTL;
	OrderListener thrOL;
	TokenDistributionCenter thrTC;
	int nbRunners = 4;

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
		brokerName = this.ctx.getBrokerName();
		if (ctx.applicationsById.values().size() > 0)
			this.emf = ctx.getTransacEMF();
		this.thrsRA = new ArrayList<RunnerAgent>();

		// Create broker service
		broker = new BrokerService();

		// Basic configuration
		broker.setBrokerName(brokerName);
		broker.setPersistent(true);
		broker.setDataDirectory(ctx.configurationDirectoryPath + File.separator + "activemq-data");
		broker.setUseJmx(false); // Not now. Later.
		broker.setDeleteAllMessagesOnStartup(purge);

		// System resources
		MemoryUsage mu = new MemoryUsage();
		mu.setLimit((1024 * 1024) * 20);

		StoreUsage stu = new StoreUsage();
		stu.setLimit((1024 * 1024) * 38);

		TempUsage tu = new TempUsage();
		tu.setLimit((1024 * 1024) * 38);

		SystemUsage su = broker.getSystemUsage();
		su.setMemoryUsage(mu);
		su.setStoreUsage(stu);
		su.setTempUsage(tu);
		broker.setSystemUsage(su);

		// Add a listener
		broker.addConnector("tcp://" + this.ctx.localUrl);

		// Add channels to other nodes
		ArrayList<String> opened = new ArrayList<String>();
		for (Application a : this.ctx.applicationsById.values()) {
			for (NodeLink nl : a.getLocalNode().getCanSendTo()) {
				if (!(nl.getMethod().equals(NodeConnectionMethod.TCP) || nl.getMethod().equals(NodeConnectionMethod.RCTRL)))
					break;
				if (opened.contains(nl.getNodeTo().getBrokerUrl()))
					break;
				opened.add(nl.getNodeTo().getBrokerUrl());

				String url = "static:(tcp://" + nl.getNodeTo().getDns() + ":" + nl.getNodeTo().getqPort() + ")";
				log.info(String.format("(%s) This broker will be able to open a channel towards %s", ctx.configurationDirectoryPath, url));
				NetworkConnector tc = broker.addNetworkConnector(url);
				tc.setDuplex(true);
				tc.setNetworkTTL(20);
			}

			for (NodeLink nl : a.getLocalNode().getCanReceiveFrom()) {
				if (!nl.getMethod().equals(NodeConnectionMethod.TCP))
					break;

				log.info(String.format("(%s) This broker should receive channels incoming from %s:%s)", ctx.configurationDirectoryPath, nl
						.getNodeFrom().getDns(), nl.getNodeFrom().getqPort()));
			}
		}

		// Start
		log.info(String.format("(%s) The message broker will now start", ctx.configurationDirectoryPath));
		try {
			broker.start();
		} catch (Exception e) {
			log.error("Failed to star the broker", e);
			throw e; // a little stupid...
		}

		// Factory
		this.factory = new ActiveMQConnectionFactory("vm://" + brokerName);

		// Connect to it...
		this.connection = factory.createConnection();
		this.connection.start();
	}

	public void registerListeners(ChronixEngine engine) throws JMSException, IOException {
		registerListeners(engine, true, true, true, true, true, true, true, true, true);
	}

	public void registerListeners(ChronixEngine engine, boolean startMeta, boolean startRunnerAgent, boolean startPipeline, boolean startRunner,
			boolean startLog, boolean startTranscient, boolean startEventListener, boolean startOrderListener, boolean startTokenDistributionCenter)
			throws JMSException, IOException {
		this.engine = engine;

		if (startMeta) {
			this.thrML = new MetadataListener();
			this.thrML.startListening(this.connection, brokerName, ctx, this.engine);
		}

		if (startRunnerAgent) {
			for (int i = 0; i < this.nbRunners; i++) {
				RunnerAgent thrRA = new RunnerAgent();
				thrRA.startListening(this.connection, brokerName, FilenameUtils.concat(ctx.configurationDirectoryPath, "JOBLOG"));
				this.thrsRA.add(thrRA);
			}
		}

		if (startEventListener && this.emf != null) {
			this.thrEL = new EventListener();
			this.thrEL.startListening(this.connection, brokerName, ctx, emf);
		}

		if (startPipeline && this.emf != null) {
			this.thrPL = new Pipeline();
			this.thrPL.startListening(this.connection, brokerName, ctx, emf);
		}

		if (startRunner && this.emf != null) {
			this.thrRU = new Runner();
			this.thrRU.startListening(this.connection, brokerName, ctx, emf);
		}

		if (startLog) {
			this.thrLL = new LogListener();
			this.thrLL.startListening(this.connection, brokerName, ctx);
		}

		if (startTranscient && this.emf != null) {
			this.thrTL = new TranscientListener();
			this.thrTL.startListening(this.connection, brokerName, ctx, emf);
		}

		if (startOrderListener && this.emf != null) {
			this.thrOL = new OrderListener();
			this.thrOL.startListening(this.connection, brokerName, ctx);
		}

		if (startTokenDistributionCenter && this.emf != null) {
			this.thrTC = new TokenDistributionCenter();
			this.thrTC.startListening(this.connection, brokerName, ctx);
		}
	}

	public void stop() {
		log.info(String.format("(%s) The message broker will now stop", this.ctx.configurationDirectoryPath));
		try {
			if (this.thrML != null)
				this.thrML.stopListening();
			if (this.thrsRA.size() > 0) {
				for (RunnerAgent ra : this.thrsRA)
					ra.stopListening();
			}
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
			if (this.thrOL != null)
				this.thrOL.stopListening();
			if (this.thrTC != null)
				this.thrTC.stopListening();

			for (NetworkConnector nc : this.broker.getNetworkConnectors()) {
				this.broker.removeNetworkConnector(nc);
			}

			this.connection.close();
			broker.stop();
			broker.waitUntilStopped();
		} catch (Exception e) {
			log.warn("an error occured while trying to stop the broker", e);
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

	public int getNbRunners()
	{
		return nbRunners;
	}

	public void setNbRunners(int nbRunners)
	{
		this.nbRunners = nbRunners;
	}
}
