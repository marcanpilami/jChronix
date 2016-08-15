package org.oxymores.chronix.engine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.network.NetworkConnector;
import org.apache.activemq.usage.MemoryUsage;
import org.apache.activemq.usage.StoreUsage;
import org.apache.activemq.usage.SystemUsage;
import org.apache.activemq.usage.TempUsage;
import org.apache.commons.io.FileUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.api.agent.MessageListenerService;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The JMS broker.
 */
@Component(configurationPid = "ServiceHost", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = false)
public class JmsServer implements MessageListenerService
{
    private static final Logger log = LoggerFactory.getLogger(JmsServer.class);

    ///////////////////////////////////////////////////////////////////////////
    // DATA
    ///////////////////////////////////////////////////////////////////////////

    //////////////////////////////////////
    // Configuration
    private String[] interfaces;
    private String brokerId;
    private File dbPath;
    private boolean clear = false;
    private Set<String> channels = new HashSet<>();
    private int maxMemMb, maxStoreMb, maxTempMb;

    //////////////////////////////////////
    // AMQ elements
    private BrokerService broker;
    private ActiveMQConnectionFactory factory;
    private Connection connection;

    private Semaphore stopped = new Semaphore(0);

    //////////////////////////////////////
    // OSGI magic fields
    ConfigurationAdmin confSrv;

    @Reference
    void setConfService(ConfigurationAdmin c)
    {
        this.confSrv = c;
    }

    //
    ///////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // DESTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    @Deactivate
    public void deactivate()
    {
        log.debug("Server has received an OSGI deactivate request");
        stopBroker();
    }

    synchronized void stopBroker()
    {
        log.info("Stopping message broker with ID " + this.brokerId);
        try
        {
            this.channels.clear();
            this.syncLinks();

            this.connection.close();
            this.broker.stop();
            this.broker.waitUntilStopped();
            this.factory = null;
            this.broker = null;

            // TCP port release is not immediate, sad.
            try
            {
                Thread.sleep(Constants.BROKER_PORT_FREEING_MS);
            }
            catch (InterruptedException e)
            {
                log.info("Interruption while waiting for port freeing");
            }

            log.debug("Broker has ended its stop sequence");
            stopped.release();
        }
        catch (Exception e)
        {
            log.warn("An error occured while trying to stop the broker", e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    @Activate
    public void activate(Map<String, String> config)
    {
        // What to listen to
        interfaces = config.getOrDefault("org.oxymores.chronix.network.interfaces", "localhost:1789").split(",");

        // Persistence store location
        dbPath = new File(config.getOrDefault("org.oxymores.chronix.network.dbpath", "./metabase/broker-data"));
        try
        {
            FileUtils.forceMkdir(dbPath);
        }
        catch (IOException e)
        {
            throw new ChronixInitializationException("cannot create or find directory " + dbPath.getAbsolutePath(), e);
        }

        // Channels to open
        for (String channel : config.getOrDefault("org.oxymores.chronix.network.channels", "").split(","))
        {
            if (!channel.isEmpty())
            {
                this.channels.add(channel.toUpperCase());
            }
        }

        // Node ID - set it if needed
        brokerId = config.getOrDefault("org.oxymores.chronix.network.nodeid", "null");
        if ("null".equals(brokerId))
        {
            brokerId = UUID.randomUUID().toString();
            try
            {
                Configuration cc = confSrv.getConfiguration("ServiceHost");
                Dictionary<String, Object> current = cc.getProperties();
                current.put("org.oxymores.chronix.network.nodeid", brokerId);
                cc.update(current);
            }
            catch (IOException e)
            {
                throw new ChronixInitializationException("Cannot set node unique ID in configuration", e);
            }
        }
        SenderHelpers.defaultBrokerName = brokerId;

        // Clear?
        clear = Boolean.parseBoolean(config.getOrDefault("org.oxymores.chronix.network.clear", "false"));

        // System usage
        maxMemMb = Integer.parseInt(config.getOrDefault("org.oxymores.chronix.network.usage.memory", "20"));
        maxStoreMb = Integer.parseInt(config.getOrDefault("org.oxymores.chronix.network.usage.store", "38"));
        maxTempMb = Integer.parseInt(config.getOrDefault("org.oxymores.chronix.network.usage.temp", "38"));

        // Config is OK - go!
        startup();
    }

    private void startup()
    {
        log.info("Starting configuration of a message broker with unique ID " + this.brokerId);
        stopped.drainPermits();

        // Class white listing (see http://activemq.apache.org/objectmessage.html)
        System.setProperty("org.apache.activemq.SERIALIZABLE_PACKAGES", "*");

        // Create broker service
        broker = new BrokerService();

        // Basic configuration
        broker.setBrokerName(this.brokerId);
        broker.setDataDirectory(this.dbPath + File.separator + "activemq-data");
        broker.setUseJmx(false);
        broker.setDeleteAllMessagesOnStartup(clear);
        broker.setEnableStatistics(false);
        broker.setPersistent(true);
        broker.setRestartAllowed(false);
        broker.setStartAsync(false);
        broker.setUseShutdownHook(false);

        // System resources
        MemoryUsage mu = new MemoryUsage();
        mu.setLimit(maxMemMb * Constants.MB);

        StoreUsage stu = new StoreUsage();
        stu.setLimit(maxStoreMb * Constants.MB);

        TempUsage tu = new TempUsage();
        tu.setLimit(maxTempMb * Constants.MB);

        SystemUsage su = broker.getSystemUsage();
        su.setMemoryUsage(mu);
        su.setStoreUsage(stu);
        su.setTempUsage(tu);
        broker.setSystemUsage(su);

        // Listeners
        for (String connector : this.interfaces)
        {
            try
            {
                log.info("Broker will listen on interface " + connector);
                TransportConnector tc = broker.addConnector("tcp://" + connector);
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
            TransportConnector tc = broker.addConnector("vm://" + this.brokerId);
            tc.setDiscoveryUri(null);
        }
        catch (Exception e1)
        {
            throw new ChronixInitializationException("Could not create a JMS VM listener connector", e1);
        }

        // Add channels to other nodes
        syncLinks();

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

        // Factory for VM connections
        this.factory = new ActiveMQConnectionFactory("vm://" + this.brokerId);

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

    private final void syncLinks() throws ChronixInitializationException
    {
        // Stop links that are not in the list anymore
        try
        {
            for (NetworkConnector nc : this.broker.getNetworkConnectors())
            {
                if (this.channels.contains(nc.getName()))
                {
                    log.info("Stopping channel towards " + nc.getName());
                    nc.stop();
                    this.broker.removeNetworkConnector(nc);
                }
            }
        }
        catch (Exception e)
        {
            throw new ChronixInitializationException("an error occured while trying to stop an outgoing link to another node", e);
        }

        // Check which channels should be created
        List<String> toCreate = new ArrayList<>();
        outer: for (String ch : this.channels)
        {
            for (NetworkConnector nc : this.broker.getNetworkConnectors())
            {
                if (nc.getName().equals(ch))
                {
                    // Found - no need to create it
                    continue outer;
                }
            }
            // Not found if here
            toCreate.add(ch);
        }

        // Create missing channels
        for (String ch : toCreate)
        {
            String url = "static:(tcp://" + ch + ")";
            log.info(String.format("The broker will open a channel towards %s", url));
            NetworkConnector tc;
            try
            {
                tc = broker.addNetworkConnector(url);
                tc.setName(ch);
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
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // PUBLIC INTERFACE
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void addChannel(String host)
    {
        this.channels.add(host.toUpperCase());
        this.syncLinks();
    }

    @Override
    public Object addMessageCallback(String queueName, MessageCallback callback, String nodeName)
    {
        return new ListenerHolder(connection, queueName, callback, nodeName);
    }

    @Override
    public void removeMessageCallback(Object callbackId)
    {
        ListenerHolder tmp = (ListenerHolder) callbackId;
        tmp.stopListening();
    }

    @Override
    public Session getNewSession()
    {
        try
        {
            return connection.createSession(true, Session.SESSION_TRANSACTED);
        }
        catch (JMSException e)
        {
            throw new ChronixException("Could not create session", e);
        }
    }

    @Override
    public void waitUntilServerIsStopped()
    {
        try
        {
            this.stopped.acquire();
            this.stopped.release();
        }
        catch (Exception e)
        {
            // Not important.
        }

    }
}
