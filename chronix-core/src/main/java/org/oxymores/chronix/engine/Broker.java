/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.NodeLink;
import org.oxymores.chronix.exceptions.ChronixInitializationException;

/*
 * Queue name: cf constants
 */
class Broker
{
    private static Logger log = Logger.getLogger(Broker.class);

    // JMS
    private String brokerName, url;
    private BrokerService broker;
    private ActiveMQConnectionFactory factory;
    private Connection connection;

    // Chronix running context
    private ChronixContext ctx;
    private ChronixEngine engine;

    // Threads
    private MetadataListener thrML;
    private List<RunnerAgent> thrsRA;
    private EventListener thrEL;
    private Pipeline thrPL;
    private RunnerManager thrRU;
    private LogListener thrLL;
    private TranscientListener thrTL;
    private OrderListener thrOL;
    private TokenDistributionCenter thrTC;
    private int nbRunners = Constants.DEFAULT_NB_RUNNER;

    Broker(ChronixEngine engine) throws ChronixInitializationException
    {
        this(engine, false);
    }

    Broker(ChronixContext ctx) throws ChronixInitializationException
    {
        this(ctx, false, true, true);
    }

    Broker(ChronixEngine engine, boolean purge) throws ChronixInitializationException
    {
        this(engine.ctx, purge, true, true);
    }

    Broker(ChronixContext ctx, boolean purge, boolean persistent, boolean tcp) throws ChronixInitializationException
    {
        this.ctx = ctx;
        this.brokerName = ctx.getLocalNode().getBrokerName();
        this.url = "vm://" + this.brokerName;

        log.info(String.format("Starting configuration of a message broker listening on %s", url));
        this.thrsRA = new ArrayList<>();

        // Create broker service
        broker = new BrokerService();

        // Basic configuration
        broker.setBrokerName(brokerName);
        broker.setDataDirectory(ctx.getContextRoot() + File.separator + "activemq-data");
        broker.setUseJmx(false);
        broker.setDeleteAllMessagesOnStartup(purge);
        broker.setEnableStatistics(false);
        broker.setPersistent(true);
        broker.setRestartAllowed(false);
        broker.setSupportFailOver(false);
        broker.setStartAsync(false);

        // System resources
        MemoryUsage mu = new MemoryUsage();
        mu.setLimit(Constants.DEFAULT_BROKER_MEM_USAGE);

        StoreUsage stu = new StoreUsage();
        stu.setLimit(Constants.DEFAULT_BROKER_STORE_USAGE);

        TempUsage tu = new TempUsage();
        tu.setLimit(Constants.DEFAULT_BROKER_TEMP_USAGE);

        SystemUsage su = broker.getSystemUsage();
        su.setMemoryUsage(mu);
        su.setStoreUsage(stu);
        su.setTempUsage(tu);
        broker.setSystemUsage(su);

        // Add a listener
        if (tcp)
        {
            try
            {
                TransportConnector tc = broker.addConnector("tcp://" + this.ctx.getLocalNode().getBrokerUrl());
                tc.setDiscoveryUri(null);
            }
            catch (Exception e1)
            {
                throw new ChronixInitializationException("Could not create a JMS listener connector", e1);
            }
        }

        // Always add VM connector.
        try
        {
            TransportConnector tc = broker.addConnector("vm://" + this.ctx.getLocalNode().getId());
            tc.setDiscoveryUri(null);
        }
        catch (Exception e1)
        {
            throw new ChronixInitializationException("Could not create a JMS listener connector", e1);
        }

        // Add channels to other nodes
        resetLinks();

        // Start
        log.info(String.format("The message broker will now start"));
        try
        {
            broker.start();
        }
        catch (Exception e)
        {
            log.error("Failed to start the broker", e);
            throw new ChronixInitializationException("Could not start the broker", e);
        }

        // Factory
        this.factory = new ActiveMQConnectionFactory("vm://" + brokerName);

        // Connect to it...
        try
        {
            this.connection = factory.createConnection();
            this.connection.start();
        }
        catch (JMSException e)
        {
            throw new ChronixInitializationException("Could not connect to the JMS broker", e);
        }
    }

    void resetContext(ChronixContext ctx) throws ChronixInitializationException
    {
        log.debug("The broker context will be reset " + ctx.getContextRoot());
        this.ctx = ctx;
        broker.setBrokerName(brokerName);
        resetLinks();
    }

    void registerListeners(ChronixEngine engine) throws JMSException, IOException, ChronixInitializationException
    {
        registerListeners(engine, true, true, true, true, true, true, true, true, true);
    }

    void registerListeners(ChronixEngine engine, boolean startMeta, boolean startRunnerAgent, boolean startPipeline, boolean startRunner, boolean startLog, boolean startTranscient,
            boolean startEventListener, boolean startOrderListener, boolean startTokenDistributionCenter) throws JMSException, IOException, ChronixInitializationException
    {
        this.engine = engine;

        if (startMeta && this.thrML == null)
        {
            this.thrML = new MetadataListener();
            this.thrML.startListening(this, this.engine);
        }

        if (startRunnerAgent && this.thrsRA.isEmpty())
        {
            for (int i = 0; i < this.nbRunners; i++)
            {
                RunnerAgent thrRA = new RunnerAgent();
                thrRA.startListening(this);
                this.thrsRA.add(thrRA);
            }
        }

        if (startEventListener && this.thrEL == null)
        {
            this.thrEL = new EventListener();
            this.thrEL.startListening(this);
        }

        if (startPipeline && this.thrPL == null)
        {
            this.thrPL = new Pipeline();
            this.thrPL.startListening(this);
        }

        if (startRunner && this.thrRU == null)
        {
            this.thrRU = new RunnerManager();
            this.thrRU.startListening(this);
        }

        if (startLog && this.thrLL == null)
        {
            this.thrLL = new LogListener();
            this.thrLL.startListening(this);
        }

        if (startTranscient && this.thrTL == null)
        {
            this.thrTL = new TranscientListener();
            this.thrTL.startListening(this);
        }

        if (startOrderListener && this.thrOL == null)
        {
            this.thrOL = new OrderListener();
            this.thrOL.startListening(this);
        }

        if (startTokenDistributionCenter && this.thrTC == null)
        {
            this.thrTC = new TokenDistributionCenter();
            this.thrTC.startListening(this);
        }
    }

    void stopEngineListeners()
    {
        log.debug("Stopping all engine threads " + ctx.getContextRoot());
        if (this.thrML != null)
        {
            this.thrML.stopListening();
            this.thrML = null;
        }
        if (this.thrEL != null)
        {
            this.thrEL.stopListening();
            this.thrEL = null;
        }
        if (this.thrPL != null)
        {
            this.thrPL.stopListening();
            this.thrPL = null;
        }
        if (this.thrRU != null)
        {
            this.thrRU.stopListening();
            this.thrRU = null;
        }
        if (this.thrLL != null)
        {
            this.thrLL.stopListening();
            this.thrLL = null;
        }
        if (this.thrTL != null)
        {
            this.thrTL.stopListening();
            this.thrTL = null;
        }
        if (this.thrOL != null)
        {
            this.thrOL.stopListening();
            this.thrOL = null;
        }
        if (this.thrTC != null)
        {
            this.thrTC.stopListening();
            this.thrTC = null;
        }
    }

    void stopRunnerAgents()
    {
        log.debug("Stopping all runner agent threads " + ctx.getContextRoot());
        if (this.thrsRA.size() > 0)
        {
            for (RunnerAgent ra : this.thrsRA)
            {
                ra.stopListening();
            }
            this.thrsRA.clear();
        }
    }

    void stopBroker()
    {
        log.info(String.format("The message broker will now stop"));
        try
        {
            stopAllOutgoingLinks();
            this.connection.close();
            this.broker.stop();
            this.broker.waitUntilStopped();
            this.factory = null;
            this.engine = null;
            log.debug("Broker has ended its stop sequence");
        }
        catch (Exception e)
        {
            log.warn("An error occured while trying to stop the broker", e);
        }
    }

    void stop()
    {
        stopRunnerAgents();
        stopEngineListeners();
        stopBroker();
    }

    void stopAllOutgoingLinks() throws ChronixInitializationException
    {
        if (this.broker.isStarted())
        {
            log.debug("Stopping all outgoing network connectors " + ctx.getContextRoot());
        }
        try
        {
            for (NetworkConnector nc : this.broker.getNetworkConnectors())
            {
                nc.stop();
                this.broker.removeNetworkConnector(nc);
            }
        }
        catch (Exception e)
        {
            throw new ChronixInitializationException("an error occured while trying to stop an outgoing link to another node", e);
        }
    }

    void resetLinks() throws ChronixInitializationException
    {
        stopAllOutgoingLinks();

        ArrayList<String> opened = new ArrayList<>();
        for (Application a : this.ctx.getApplications())
        {
            for (NodeLink nl : a.getLocalNode().getCanSendTo())
            {
                // Only if TCP or RCTRL
                // Only if not already opened
                if (!(nl.getMethod().equals(NodeConnectionMethod.TCP) || nl.getMethod().equals(NodeConnectionMethod.RCTRL)) || opened.contains(nl.getNodeTo().getBrokerUrl()))
                {
                    break;
                }
                opened.add(nl.getNodeTo().getBrokerUrl());

                String url = "static:(tcp://" + nl.getNodeTo().getDns() + ":" + nl.getNodeTo().getqPort() + ")";
                log.info(String.format("The broker will open a channel towards %s", url));
                NetworkConnector tc;
                try
                {
                    tc = broker.addNetworkConnector(url);
                    if (broker.isStarted())
                    {
                        // Connectors are auto started on broker startup, but not started otherwise.
                        tc.start();
                    }
                }
                catch (Exception e)
                {
                    throw new ChronixInitializationException("Could not create a JMS network connector", e);
                }
                tc.setDuplex(true);
                tc.setAlwaysSyncSend(true);
                // tc.setNetworkTTL(Constants.DEFAULT_BROKER_NETWORK_CONNECTOR_TTL_S);
            }

            for (NodeLink nl : a.getLocalNode().getCanReceiveFrom())
            {
                if (nl.getMethod().equals(NodeConnectionMethod.TCP))
                {
                    log.info(String.format("The broker should receive channels incoming from %s:%s", nl.getNodeFrom().getDns(), nl.getNodeFrom().getqPort()));
                }
            }
        }
    }

    BrokerService getBroker()
    {
        return this.broker;
    }

    Connection getConnection()
    {
        return connection;
    }

    void purgeAllQueues() throws JMSException
    {
        log.warn("purge all queues on broker was called");
        try
        {
            broker.deleteAllMessages();
        }
        catch (IOException e1)
        {
            log.warn("An error occurend while purging queues. Not a real problem - the engine will still run perfectly. Still, please report this bug.", e1);
        }
    }

    ChronixContext getCtx()
    {
        return ctx;
    }

    String getBrokerName()
    {
        return this.brokerName;
    }

    void setNbRunners(int nbRunners)
    {
        this.nbRunners = nbRunners;
    }
}
