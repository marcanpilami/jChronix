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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.oxymores.chronix.engine.data.RunDescription;
import org.oxymores.chronix.engine.data.RunResult;

class RunnerAgent extends BaseListener
{
    private static Logger log = Logger.getLogger(RunnerAgent.class);

    private MessageProducer jmsProducer;
    private String logDbPath;

    void startListening(Broker broker) throws JMSException, IOException
    {
        this.init(broker, false, false);
        log.info(String.format("(%s) Starting a runner agent", brokerName));

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
        RunResult res = null;

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

        if (!rd.getHelperExecRequest())
        {
            log.info(String.format("Running command %s", rd.getCommand()));
        }
        else
        {
            log.debug(String.format("Running helper internal command %s", rd.getCommand()));
        }

        // Log file (only if true run)
        String logFilePath = null;
        String logFileName = null;
        Date start = new Date();
        if (!rd.getHelperExecRequest())
        {
            SimpleDateFormat myFormatDir = new SimpleDateFormat("yyyyMMdd");
            SimpleDateFormat myFormatFile = new SimpleDateFormat("yyyyMMddhhmmssSSS");
            String dd = myFormatDir.format(start);
            String logFileDateDir = FilenameUtils.concat(this.logDbPath, dd);
            if (!(new File(logFileDateDir)).exists() && !(new File(logFileDateDir)).mkdir())
            {
                log.fatal("Could not create log directory, failing engine");
                this.broker.stop();
                jmsRollback();
                return;
            }
            logFileName = String.format("%s_%s_%s_%s.log", myFormatFile.format(start), rd.getPlaceName().replace(" ", "-"), rd
                    .getActiveSourceName().replace(" ", "-"), rd.getId1());
            logFilePath = FilenameUtils.concat(logFileDateDir, logFileName);
        }

        // Run the command according to its method
        if (rd.getMethod().equals(Constants.JD_METHOD_SHELL))
        {
            res = RunnerShell.run(rd, logFilePath, !rd.getHelperExecRequest(), rd.getShouldSendLogFile());
        }
        else
        {
            res = new RunResult();
            res.returnCode = -1;
            res.logStart = String.format("An unimplemented exec method (%s) was called!", rd.getMethod());
            log.error(String.format("An unimplemented exec method (%s) was called! Job will be failed.", rd.getMethod()));
        }
        res.start = start;
        res.end = new Date();
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
                    log.warn("A log file was too big and will not be sent. Only the full log file will be missing - the launch will still appear in the console.");
                }
            }
            catch (Exception e1)
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
