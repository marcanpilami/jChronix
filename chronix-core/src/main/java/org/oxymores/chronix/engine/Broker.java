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
import java.util.UUID;

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
import org.oxymores.chronix.core.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Queue name: cf constants
 */
class Broker
{
    private static Logger log = LoggerFactory.getLogger(Broker.class);

    // JMS
    private String brokerName;
    private BrokerService broker;
    private ActiveMQConnectionFactory factory;
    private Connection connection;

    private String brokerDataDirectory;

    // Chronix running context
    // private ChronixContext ctx;
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

    Broker(ChronixEngine engine, String brokerDataDirectory, boolean purge, boolean persistent, boolean tcp)
            throws ChronixInitializationException
    {
        this.engine = engine;
        this.brokerDataDirectory = brokerDataDirectory;
        this.brokerName = engine.getLocalNode().getBrokerName();

        log.info(String.format("Starting configuration of a message broker listening on vm://%s", this.brokerName));
        this.thrsRA = new ArrayList<>();

        // Class white listing (see http://activemq.apache.org/objectmessage.html)
        System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES",
                "java.util,java.lang,org.joda.time," + "org.oxymores.chronix.core," + "org.oxymores.chronix.core.active,"
                        + "org.oxymores.chronix.core.timedata," + "org.oxymores.chronix.core.transactional,"
                        + "org.oxymores.chronix.engine.data," + "org.oxymores.chronix.engine.helpers,"
                        + "org.oxymores.chronix.engine.modularity.runner");

        // Create broker service
        broker = new BrokerService();

        // Basic configuration
        broker.setBrokerName(brokerName);
        broker.setDataDirectory(this.brokerDataDirectory + File.separator + "activemq-data");
        broker.setUseJmx(false);
        broker.setDeleteAllMessagesOnStartup(purge);
        broker.setEnableStatistics(false);
        broker.setPersistent(true);
        broker.setRestartAllowed(false);
        broker.setStartAsync(false);
        broker.setUseShutdownHook(false);

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
            for (ExecutionNodeConnectionAmq conn : this.engine.getLocalNode().getConnectionParameters(ExecutionNodeConnectionAmq.class))
            {
                try
                {
                    TransportConnector tc = broker.addConnector("tcp://" + conn.getDns() + ":" + conn.getqPort());
                    tc.setDiscoveryUri(null);
                }
                catch (Exception e1)
                {
                    throw new ChronixInitializationException("Could not create a JMS listener connector", e1);
                }
            }
        }

        // Always add VM connector.
        try
        {
            TransportConnector tc = broker.addConnector("vm://" + this.engine.getLocalNode().getId());
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
        // this.factory.setObjectMessageSerializationDefered(true);

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

    void registerListeners(ChronixEngine engine) throws JMSException, IOException, ChronixInitializationException
    {
        registerListeners(engine, true, true, true, true, true, true, true, true, true);
    }

    void registerListeners(ChronixEngine engine, boolean startMeta, boolean startRunnerAgent, boolean startPipeline, boolean startRunner,
            boolean startLog, boolean startTranscient, boolean startEventListener, boolean startOrderListener,
            boolean startTokenDistributionCenter) throws JMSException, IOException, ChronixInitializationException
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
        log.debug("Stopping all engine threads");
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
        log.debug("Stopping all runner agent threads");
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
            log.debug("Stopping all outgoing network connectors");
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

    final void resetLinks() throws ChronixInitializationException
    {
        stopAllOutgoingLinks();

        ArrayList<UUID> opened = new ArrayList<>();

        for (ExecutionNodeConnectionAmq conn : this.engine.getDirectNeigbhours())
        {
            // Only if not already opened
            if (opened.contains(conn.getId()))
            {
                break;
            }
            opened.add(conn.getId());

            String url = "static:(tcp://" + conn.getDns() + ":" + conn.getqPort() + ")";
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

        /*
         * for (ExecutionNode en : a.getLocalNode().getCanReceiveFrom()) { log.info(String.format(
         * "The broker should receive channels incoming from %s", en.getName())); }
         */

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
            log.warn(
                    "An error occurend while purging queues. Not a real problem - the engine will still run perfectly. Still, please report this bug.",
                    e1);
        }
    }

    String getBrokerName()
    {
        return this.brokerName;
    }

    void setNbRunners(int nbRunners)
    {
        this.nbRunners = nbRunners;
    }

    public ChronixEngine getEngine()
    {
        return this.engine;
    }
}
