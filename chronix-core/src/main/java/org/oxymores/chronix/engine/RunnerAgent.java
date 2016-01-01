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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.engine.modularity.runner.RunDescription;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.oxymores.chronix.engine.modularity.runner.RunnerApi;
import org.slf4j.LoggerFactory;

class RunnerAgent extends BaseListener
{
    private static final Logger log = LoggerFactory.getLogger(RunnerAgent.class);

    private static final DateTimeFormatter JODA_LOG_FORMATTER = DateTimeFormat.forPattern("dd/MM HH:mm:ss");
    private static final DateTimeFormatter JODA_DIR_FORMATTER = DateTimeFormat.forPattern("yyyyMMdd");
    private static final DateTimeFormatter JODA_FILE_FORMATTER = DateTimeFormat.forPattern("yyyyMMddhhmmssSSS");

    private MessageProducer jmsProducer;
    private String logDbPath;

    // Optional OSGI stuff
    private Map<String, ServiceTracker<RunnerApi, RunnerApi>> runnerTrackers = new HashMap<>();
    private boolean isOsgi = FrameworkUtil.getBundle(RunnerAgent.class) != null;

    void startListening(Broker broker) throws JMSException, IOException
    {
        this.init(broker);
        log.info(String.format("Starting a runner agent"));

        // Log repository
        this.logDbPath = FilenameUtils.normalize(FilenameUtils.concat(ctx.getContextRoot(), "LOCALJOBLOG"));
        if (!(new File(this.logDbPath)).exists())
        {
            (new File(this.logDbPath)).mkdir();
        }

        // Producer to send run results
        this.jmsProducer = this.jmsSession.createProducer(null);

        // Queue listener
        qName = String.format(Constants.Q_RUNNER, brokerName);
        this.subscribeTo(qName);
    }

    @Override
    public void onMessage(Message msg)
    {
        log.info("Run request received");
        ObjectMessage omsg = (ObjectMessage) msg;
        RunDescription rd;
        RunResult res;

        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof RunDescription))
            {
                log.warn("An object was received on the runner queue but was not a RunDescription! Ignored.");
                jmsCommit();
                return;
            }
            rd = (RunDescription) o;
        }
        catch (JMSException e)
        {
            log.error("An error occurred during RunDescription reception. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        // Log file (only if true run)
        String logFilePath = null;
        String logFileName = null;
        DateTime start = DateTime.now();
        if (!rd.getHelperExecRequest())
        {
            String logFileDateDir = FilenameUtils.concat(this.logDbPath, start.toString(JODA_DIR_FORMATTER));
            if (!(new File(logFileDateDir)).exists() && !(new File(logFileDateDir)).mkdir())
            {
                log.error("Could not create log directory, failing engine");
                this.broker.stop();
                jmsRollback();
                return;
            }
            logFileName = String.format("%s_%s_%s_%s.log", start.toString(JODA_FILE_FORMATTER), rd.getPlaceName().replace(" ", "-"),
                    rd.getActiveSourceName().replace(" ", "-"), rd.getId1());
            logFilePath = FilenameUtils.concat(logFileDateDir, logFileName);
        }
        rd.setLogFilePath(logFilePath);
        rd.setStoreLogFile(!rd.getHelperExecRequest());
        rd.setReturnFullerLog(rd.getShouldSendLogFile());

        // Run the command according to its method
        RunnerApi runner = null;
        if (isOsgi)
        {
            // Get or create the tracker
            ServiceTracker<RunnerApi, RunnerApi> tracker = runnerTrackers.get(rd.getPluginSelector());
            if (tracker == null)
            {
                BundleContext ctx = FrameworkUtil.getBundle(RunnerAgent.class).getBundleContext();
                Filter filter = null;
                try
                {
                    filter = ctx.createFilter("(&(objectClass=com.eclipsesource.MyInterface)(shell=" + rd.getPluginSelector() + "))");
                }
                catch (InvalidSyntaxException e)
                {
                    log.warn("a job was defined to run with an invalid capability '" + rd.getPluginSelector() + "'. It is ignored.", e);
                    jmsCommit();
                    return;
                }
                tracker = new ServiceTracker<>(ctx, filter, null);
                runnerTrackers.put(rd.getPluginSelector(), tracker);
            }

            // Get the service
            runner = tracker.getService();
            if (runner == null)
            {
                res = new RunResult();
                res.returnCode = -1;
                res.logStart = String.format("An unimplemented exec plugin (%s) was called!", rd.getPluginSelector());
                log.warn("There are no runners able to run a job of type " + rd.getPluginSelector() + ". Job will be failed.");
            }
            else
            {
                res = runner.run(rd);
            }
        }
        else
        {
            // This only happens during tests. The only runner we ever need during tests is the shell runner.
            try
            {
                runner = (RunnerApi) Class.forName("org.oxymores.chronix.engine.modularity.runnerimpl.RunnerShell").newInstance();
                res = runner.run(rd);
            }
            catch (Exception e)
            {
                log.error("Could not load shell runner (which should only be loaded during tests, never during normal run", e);
                jmsCommit();
                return;
            }
        }

        // Internal stuff with dates and IDs.
        res.start = start;
        res.end = DateTime.now();
        res.logFileName = logFileName;
        res.logPath = logFilePath;

        // Copy the engine ids - that way it will be able to identify the launch
        // Part of the ids are in the JMS correlation id too
        res.id1 = rd.getId1();
        res.id2 = rd.getId2();
        res.outOfPlan = rd.getOutOfPlan();

        // Send the result!
        Message response;
        if (!rd.getHelperExecRequest())
        {
            InputStream is = null;
            try
            {
                response = jmsSession.createObjectMessage(res);
                response.setJMSCorrelationID(msg.getJMSCorrelationID());
                jmsProducer.send(msg.getJMSReplyTo(), response);

                if (res.logSizeBytes <= 500000)
                {
                    response = jmsSession.createBytesMessage();
                    byte[] bytes = new byte[(int) res.logSizeBytes];
                    is = new FileInputStream(res.logPath);
                    is.read(bytes);
                    IOUtils.closeQuietly(is);

                    ((BytesMessage) response).writeBytes(bytes);
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
            catch (JMSException | IOException e1)
            {
                log.error("The runner was not able to send the result of an execution to the engine. This may corrupt plan execution!", e1);
                IOUtils.closeQuietly(is);
            }
        }
        else
        {
            try
            {
                response = jmsSession.createTextMessage(res.logStart);
                response.setJMSCorrelationID(msg.getJMSCorrelationID());
                jmsProducer.send(msg.getJMSReplyTo(), response);
            }
            catch (JMSException e1)
            {
                log.error(
                        "The runner was not able to send the result of a parameter resolution to the engine. This may corrupt plan execution!",
                        e1);
            }
        }

        jmsCommit();
    }
}
