package org.oxymores.chronix.agent.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.agent.command.api.CommandResult;
import org.oxymores.chronix.agent.command.api.CommandRunner;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.api.agent.MessageListenerService;
import org.oxymores.chronix.api.prm.AsyncParameterResult;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "agent", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CommandAgent implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(CommandAgent.class);

    private static final DateTimeFormatter JODA_DIR_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
    private static final DateTimeFormatter JODA_FILE_FORMATTER = DateTimeFormat.forPattern("yyyyMMddhhmmssSSS");

    private String logDbPath;
    private String nodeName;

    private Object listenerHandle;

    private Map<String, ServiceTracker<CommandRunner, CommandRunner>> runnerTrackers = new HashMap<>();

    ///////////////////////////////////////////////////////////
    // Magic OSGI fields
    private MessageListenerService broker;

    @Reference
    protected void setBroker(MessageListenerService b)
    {
        log.debug("Setting broker inside command agent");
        this.broker = b;
    }
    //
    ///////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    @Activate
    public void activate(Map<String, String> configuration)
    {
        nodeName = configuration.getOrDefault("chronix.cluster.node.name", "local");
        String metabase = configuration.getOrDefault("chronix.repository.path", "./metabase");
        logDbPath = FilenameUtils.concat(metabase, "AGENTJOBLOG");

        startListening();
    }

    @Deactivate
    public void deactivate()
    {
        if (listenerHandle != null)
        {
            this.broker.removeMessageCallback(listenerHandle);
        }

        for (ServiceTracker<CommandRunner, CommandRunner> tr : runnerTrackers.values())
        {
            tr.close();
        }
    }

    private void startListening()
    {
        log.info("Starting a runner agent");

        // Log repository
        if (!(new File(this.logDbPath)).exists())
        {
            (new File(this.logDbPath)).mkdirs();
        }

        // Queue listener
        listenerHandle = this.broker.addMessageCallback("Q." + nodeName + ".RUNNER", this, nodeName + "-agent");

        // Plugin tracker

    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        log.info("Shell command run request received");
        ObjectMessage omsg = (ObjectMessage) msg;
        CommandDescription cd;
        CommandResult res;

        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof CommandDescription))
            {
                log.warn("An object was received on the runner queue but was not a CommandDescription! Ignored.");
                return;
            }
            cd = (CommandDescription) o;
        }
        catch (Exception e)
        {
            throw new ListenerRollbackException(
                    "An error occurred during CommandDescription reception. Message will stay in queue and will be analysed later", e);
        }

        // Log file (only if true run)
        String logFilePath = null;
        String logFileName = null;
        DateTime start = DateTime.now();
        if (cd.isStoreLogFile())
        {
            String logFileDateDir = FilenameUtils.concat(this.logDbPath, start.toString(JODA_DIR_FORMATTER));
            if (!(new File(logFileDateDir)).exists() && !(new File(logFileDateDir)).mkdir())
            {
                this.deactivate();
                throw new ListenerRollbackException("Could not create log directory, failing shell agent. Run request is not lost.", null);
            }
            logFileName = String.format("%s_%s.log", start.toString(JODA_FILE_FORMATTER), cd.getLaunchId());
            logFilePath = FilenameUtils.concat(logFileDateDir, logFileName);
        }
        cd.setLogFilePath(logFilePath);

        // Run the command according to its method
        CommandRunner runner;

        // Get or create the tracker
        ServiceTracker<CommandRunner, CommandRunner> tracker = runnerTrackers.get(cd.getRunnerCapability());
        if (tracker == null)
        {
            BundleContext ctx = FrameworkUtil.getBundle(CommandAgent.class).getBundleContext();
            Filter filter = null;
            try
            {
                filter = ctx
                        .createFilter("(&(objectClass=" + CommandRunner.class.getName() + ")(target=" + cd.getRunnerCapability() + "))");
                log.debug(filter.toString());
            }
            catch (InvalidSyntaxException e)
            {
                log.warn("a job was defined to run with an invalid capability '" + cd.getRunnerCapability() + "'. It is ignored.", e);
                return;
            }
            tracker = new ServiceTracker<>(ctx, filter, null);
            tracker.open();
            runnerTrackers.put(cd.getRunnerCapability(), tracker);
        }

        // Get the service
        runner = tracker.getService();
        if (runner == null)
        {
            res = new CommandResult();
            res.returnCode = -1;
            res.logStart = String.format("An unimplemented exec plugin (%s) was called!", cd.getRunnerCapability());
            log.warn("There are no runners able to run a job of type " + cd.getRunnerCapability() + ". Job will be failed.");
        }
        else
        {
            res = runner.run(cd);
        }

        // Internal stuff with dates and IDs.
        res.start = start;
        res.end = DateTime.now();
        res.logPath = logFilePath;

        // Send the result!
        Message response;
        InputStream is = null;
        try
        {
            // Create a result
            Serializable resMsg = null;
            if (!cd.isParameter())
            {
                EventSourceRunResult er = new EventSourceRunResult();
                er.end = res.end;
                er.fullerLog = res.fullerLog;
                er.logPath = res.logPath;
                er.logSizeBytes = res.logSizeBytes;
                er.logStart = res.logStart;
                er.newEnvVars = res.newEnvVars;
                er.returnCode = res.returnCode;
                resMsg = er;
            }
            else
            {
                AsyncParameterResult apr = new AsyncParameterResult();
                apr.requestId = cd.getLaunchId();
                apr.result = res.logStart;
                apr.success = res.returnCode == 0;
                resMsg = apr;
            }

            // Send it
            response = jmsSession.createObjectMessage(resMsg);
            response.setObjectProperty("launchId", cd.getLaunchId().toString());
            jmsProducer.send(msg.getJMSReplyTo(), response);

            if (res.logPath != null && Files.exists(Paths.get(res.logPath)))
            {
                if (res.logSizeBytes <= 500000)
                {
                    response = jmsSession.createBytesMessage();
                    byte[] bytes = new byte[(int) (long) res.logSizeBytes]; // Erk. But res.logSizeBytes <= 500000 so it's OK.
                    is = new FileInputStream(res.logPath);
                    int readBytes = is.read(bytes);
                    IOUtils.closeQuietly(is);

                    if (readBytes > 0)
                    {
                        ((BytesMessage) response).writeBytes(bytes, 0, readBytes);
                    }
                    response.setJMSCorrelationID(msg.getJMSCorrelationID());
                    response.setStringProperty("FileName", logFileName);
                    jmsProducer.send(msg.getJMSReplyTo(), response);
                }
                else
                {
                    log.warn(
                            "A log file was too big and will not be sent. Only the full log file will be missing - the launch will still appear in the console.");
                }
            }
        }
        catch (JMSException | IOException e1)
        {
            log.error("The runner was not able to send the result of an execution to the engine. This may corrupt plan execution!", e1);
            IOUtils.closeQuietly(is);
        }

        /*
         * else { try { response = jmsSession.createTextMessage(res.logStart); response.setJMSCorrelationID(msg.getJMSCorrelationID());
         * jmsProducer.send(msg.getJMSReplyTo(), response); } catch (JMSException e1) { log.error(
         * "The runner was not able to send the result of a parameter resolution to the engine. This may corrupt plan execution!", e1); } }
         */
    }

}
