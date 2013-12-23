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
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.wapi.JettyServer;

public class Scheduler
{
    private static Logger log = Logger.getLogger(Scheduler.class);

    // A pointer on the latest launched engine - for tests only, so package visibility.
    static ChronixEngine handler = null;

    /**
     * Start function for service/Daemon wrapper
     * 
     * @param args
     * @throws Exception
     */
    static void start(String[] args)
    {
        main(args);
    }

    /**
     * Stop function for service/Daemon wrapper
     * 
     * @param args
     * @throws Exception
     */
    static void stop(String[] args)
    {
        if (handler != null)
        {
            handler.stopEngine();
            handler.waitForStopEnd();
        }
    }

    public static void main(String[] args)
    {
        // Scheduler only uses & stores UTC dates.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Load properties
        Properties applicationProps = new Properties();
        FileInputStream fis = null;
        try
        {
            File f = new File("./conf/scheduler.properties");
            System.out.println("Trying to locate file " + f.getAbsolutePath());
            fis = new FileInputStream(f);
            applicationProps.load(fis);
            IOUtils.closeQuietly(fis);
        }
        catch (FileNotFoundException e)
        {
            // No properties file means we use default params
            System.out.println("No configuration file found at this place - using default file");
            IOUtils.closeQuietly(fis);
            InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler.properties");
            try
            {
                applicationProps.load(in);
                in.close();
            }
            catch (IOException ex)
            {
                System.out.println("Could not load default parameter file - defaults specified in the Java code will be used");
            }
        }
        catch (IOException e)
        {
            IOUtils.closeQuietly(fis);
            System.out.println("File scheduler.properties is invalid");
            return;
        }
        finally
        {
            IOUtils.closeQuietly(fis);
        }

        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = "localhost";
        }

        boolean startEngine = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.engine", "true"));
        boolean startWS = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.webserver", "true"));
        boolean startRunner = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.runner", "true"));
        String repoPath = applicationProps.getProperty("jChronix.repository.path", ".");
        String mainDataInterface = applicationProps.getProperty("jChronix.engine.mainDataInteface", hostname);
        int mainDataPort = Integer.parseInt(applicationProps.getProperty("jChronix.engine.mainDataPort", "1789"));
        String transacUnit = applicationProps.getProperty("jChronix.engine.transacPersistenceUnitName", "TransacUnit");
        String historyUnit = applicationProps.getProperty("jChronix.engine.historyPersistenceUnitName", "HistoryUnit");
        int nbRunner = Integer.parseInt(applicationProps.getProperty("jChronix.runner.maxjobs", "10"));
        String logLevel = applicationProps.getProperty("jChronix.log.level", "DEBUG");

        // Conf dir?
        new File(repoPath).mkdir();
        new File(repoPath + "/database").mkdir();

        // Log level
        Logger.getRootLogger().setLevel(Level.toLevel(logLevel));
        log.info("Logging is set at level " + Level.toLevel(logLevel).toString());

        // Start Chronix event engine
        ChronixEngine e = null;
        if (startEngine || startRunner)
        {
            log.info("Runner mode: " + (!startEngine && startRunner));
            log.info("Event engine should start: " + startEngine);
            log.info("JMS port: " + mainDataPort);
            log.info("Repository: " + repoPath);
            e = new ChronixEngine(repoPath, mainDataInterface + ":" + mainDataPort, transacUnit, historyUnit, !startEngine && startRunner,
                    nbRunner, repoPath + "/database/hsql_history.db", repoPath + "/database/hsql_transac.db");
            e.start();
            e.waitForInitEnd();
        }

        // Start web server
        if (startEngine && startWS)
        {
            JettyServer server = new JettyServer(e.getContext());
            server.start();
        }

        handler = e;
    }
}
