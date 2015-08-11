package org.oxymores.chronix.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.planbuilder.MaintenanceApplication;
import org.oxymores.chronix.planbuilder.OperationsApplication;
import static org.oxymores.chronix.planbuilder.PlanBuilder.buildExecutionNode;
import static org.oxymores.chronix.planbuilder.PlanBuilder.buildPlace;

public class Scheduler
{
    private static final Logger log = Logger.getLogger(Scheduler.class);

    // A pointer on the latest launched engine - for tests only, so package visibility.
    static ChronixEngine handler = null;
    static JettyContainer c = null;
    static Thread shutdownHook;

    /**
     * Start function for service/Daemon wrapper
     *
     * @param args
     */
    static void start(String[] args)
    {
        main(args);
    }

    /**
     * Stop function for service/Daemon wrapper
     *
     * @param args
     */
    static void stop(String[] args)
    {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
        if (handler != null)
        {
            handler.stopEngine();
            handler.waitForStopEnd();
        }
        if (c != null)
        {
            c.stop();
        }
    }

    public static void main(String[] args)
    {
        // Test arguments
        File f;
        InputStream in = null;
        try
        {
            if (args != null && args.length == 1)
            {
                f = new File(args[0]);
                in = new FileInputStream(f);
            }
            else
            {
                f = new File("./conf/scheduler.properties");
                if (!f.canRead())
                {
                    // For easier tests, also look on the classpath.
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler.properties");
                    if (in == null)
                    {
                        throw new FileNotFoundException("not in classpath nor in ./conf/scheduler.properties");
                    }
                }
                else
                {
                    in = new FileInputStream(f);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            log.fatal("Cannot read configuration file");
            throw new ChronixInitializationException("Configuration file was not found", e);
        }

        // Scheduler only uses & stores UTC dates.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Load properties
        Properties applicationProps = new Properties();
        try
        {
            log.info("Loading configuration file at " + f.getAbsolutePath());
            applicationProps.load(in);
        }
        catch (IOException e)
        {
            log.fatal("File scheduler.properties is invalid", e);
            System.exit(1);
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }

        // Get local hostname
        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = "localhost";
        }

        // Startup parameters
        String mode = applicationProps.getProperty("jChronix.mode", "SCHEDULER");

        String nodeName = applicationProps.getProperty("jChronix.cluster.node.name", null);
        String feederDns = applicationProps.getProperty("jChronix.cluster.bootstrap.dns", null);
        int feederPort = Integer.parseInt(applicationProps.getProperty("jChronix.cluster.node.name", "1789"));

        String bootstrapDns = applicationProps.getProperty("jChronix.startup.bootstrap.dns", "localhost");
        int bootstrapQPort = Integer.parseInt(applicationProps.getProperty("jChronix.startup.bootstrap.qPort", "1789"));
        int bootstrapWSPort = Integer.parseInt(applicationProps.getProperty("jChronix.startup.bootstrap.wsPort", "1790"));

        //String dbPath = new File(applicationProps.getProperty("jChronix.repository.path", "./metabase")).getAbsolutePath();
        String dbPath = applicationProps.getProperty("jChronix.repository.path", "./metabase");

        String logLevel = applicationProps.getProperty("jChronix.log.level", "INFO");
        int nbRunners = Integer.parseInt(applicationProps.getProperty("jChronix.runner.maxjobs", "5"));

        // Log level
        Logger.getRootLogger().setLevel(Level.toLevel(logLevel));
        log.info("Logging is set at level " + Level.toLevel(logLevel).toString());

        ChronixEngine e = new ChronixEngine(dbPath, nodeName, mode.equals("RUNNER"), nbRunners);

        // init the metabase
        File dbFile = new File(dbPath);
        if (!dbFile.canExecute() && !dbFile.mkdir())
        {
            log.fatal("Could not create directory " + dbFile.getAbsolutePath());
            throw new ChronixInitializationException("metabase could not be created");
        }

        log.info("Mode: " + mode);
        if (nodeName != null && feederDns != null)
        {
            // This is a new node inside a network. It will init its metabase from a feeder node.
            ExecutionNode feeder = new ExecutionNode();
            feeder.setDns(feederDns);
            feeder.setqPort(feederPort);
            e.setFeeder(feeder);

            log.info("Node name: " + nodeName);
        }
        else if (mode.equals("SCHEDULER") && ChronixContext.hasNetworkFile(dbPath))
        {
            // Either a single or a network node - if it has a network file (copied manually, from a previous boot, ...) don't touch the metabase.
            if (nodeName == null)
            {
                nodeName = hostname;
            }

            e.setLocalNodeName(nodeName);
            log.info("Node name: " + nodeName);
        }
        else if (mode.equals("SCHEDULER"))
        {
            // No network data, no mean to get it - consider this is the first node in the network (= a single node)
            // A single node with a corresponding Place
            nodeName = hostname;
            Network n = new Network();
            ExecutionNode n1 = buildExecutionNode(n, nodeName, bootstrapDns, bootstrapQPort);
            n1.setWsPort(bootstrapWSPort);
            n1.setConsole(true);
            buildPlace(n, nodeName, n1);
            ChronixContext.saveNetwork(n, dbFile);

            Application a = OperationsApplication.getNewApplication(n.getPlace(nodeName));
            ChronixContext.saveApplicationAndMakeCurrent(a, dbFile);

            a = MaintenanceApplication.getNewApplication(n.getPlace(nodeName));
            ChronixContext.saveApplicationAndMakeCurrent(a, dbFile);

            e.setLocalNodeName(nodeName);
            log.info("Node name: " + nodeName);
        }

        e.start();
        e.waitForInitEnd();

        // Jetty
        c = new JettyContainer(dbPath, e.getContext().getLocalNode().getId().toString(), e.getContext().getLocalNode().getWsPort());

        // Save the engine in order to be able to stop it later
        handler = e;

        // Register CTRL-C handler
        shutdownHook = new Thread()
        {
            @Override
            public void run()
            {
                Scheduler.stop(null);
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
}
