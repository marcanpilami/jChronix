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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.RunDescription;
import org.oxymores.chronix.core.RunResult;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.context.EngineCbRun;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.engine.data.TokenRequest.TokenRequestType;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * This manager receives {@link PipelineJob}s that have been fully cleared for running. It is responsible for handling their parameters and
 * their results.<br>
 * <strong>Note: </strong> this class cannot be multi-instantiated - there must be only one RunnerManager. Due to parameter resolution
 * cache.
 *
 */
public class RunnerManager implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(RunnerManager.class);

    private Destination destEndJob;
    private String logDbPath;
    private ChronixContextMeta ctxMeta;
    private ChronixContextTransient ctxDb;
    private ChronixEngine e;

    // The list of jobs waiting for parameter resolution
    private List<PipelineJob> resolving;

    private MessageProducer jmsProducer;

    RunnerManager(ChronixEngine e, ChronixContextMeta ctxMeta, ChronixContextTransient ctxDb, String logPath)
    {
        this.ctxDb = ctxDb;
        this.ctxMeta = ctxMeta;
        this.e = e;

        // Log repository
        this.logDbPath = FilenameUtils.normalize(FilenameUtils.concat(logPath, "GLOBALJOBLOG"));
        File logDb = new File(this.logDbPath);
        if (!logDb.exists())
        {
            try
            {
                Files.createDirectory(logDb.toPath());
            }
            catch (IOException ex)
            {
                throw new ChronixInitializationException("Could not create directory " + this.logDbPath, ex);
            }
        }

        // Internal queue
        resolving = new ArrayList<>();
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        this.jmsProducer = jmsProducer;

        if (msg instanceof ObjectMessage)
        {
            ObjectMessage omsg = (ObjectMessage) msg;
            try
            {
                Object o = omsg.getObject();
                if (o instanceof PipelineJob)
                {
                    PipelineJob pj = (PipelineJob) o;
                    log.debug(String.format("Job execution %s request was received", pj.getId()));
                    recvPJ(pj, jmsSession);
                    return;
                }
                else if (o instanceof RunResult)
                {
                    RunResult rr = (RunResult) o;
                    recvRR(rr, jmsSession);
                    return;
                }
                else if (o instanceof EventSourceRunResult)
                {
                    EventSourceRunResult esrr = (EventSourceRunResult) o;
                    UUID launchId = UUID.fromString((String) msg.getObjectProperty("launchId"));
                    recvESRR(esrr, jmsSession, launchId);
                    return;
                }
                else
                {
                    log.warn("An object was received by the Runner that was not of a valid type. It will be ignored.");
                    return;
                }

            }
            catch (JMSException e)
            {
                throw new ListenerRollbackException(
                        "An error occurred during job reception. Message will stay in queue and will be analysed later", e);
            }
        }
        else if (msg instanceof TextMessage)
        {
            TextMessage tmsg = (TextMessage) msg;
            try
            {
                recvTextMessage(tmsg, jmsSession);
            }
            catch (JMSException e)
            {
                log.error("An error occurred during parameter resolution. Element will be ignored.", e);
                return;
            }
        }
        else if (msg instanceof BytesMessage)
        {
            // log file reception
            BytesMessage bmsg = (BytesMessage) msg;
            String fn = "dump.txt";
            try
            {
                fn = bmsg.getStringProperty("FileName");
            }
            catch (JMSException e)
            {
                log.error("An log file was sent without a FileName property. It will be lost. Will not impact the scheduler itself.", e);
                return;
            }

            try (FileOutputStream fos = new FileOutputStream(new File(FilenameUtils.concat(this.logDbPath, fn))))
            {
                int l = (int) bmsg.getBodyLength();
                byte[] r = new byte[l];
                bmsg.readBytes(r);
                IOUtils.write(r, fos);
            }
            catch (Exception e)
            {
                log.error("An error has occured while receiving a log file. It will be lost. Will not impact the scheduler itself.", e);
            }
        }
    }

    // Called within JMS transaction. Don't commit here.
    private void recvTextMessage(TextMessage tmsg, Session jmsSession) throws JMSException
    {
        String res = tmsg.getText();
        String cid = tmsg.getJMSCorrelationID();

        String pjid = cid.split("\\|")[0];
        String paramid = cid.split("\\|")[1];

        // Get the PipelineJob
        PipelineJob resolvedJob = null;
        for (PipelineJob pj : this.resolving)
        {
            if (pj.getId().toString().equals(pjid))
            {
                resolvedJob = pj;
                break;
            }
        }
        if (resolvedJob == null)
        {
            log.error("received a param resolution for a job that is not in queue - ignored");
            return;
        }

        // Get the parameter awaiting resolution
        int paramIndex = -1;
        ArrayList<Parameter> prms = resolvedJob.getActive(ctxMeta).getParameters();
        for (int i = 0; i < prms.size(); i++)
        {
            if (prms.get(i).getId().toString().equals(paramid))
            {
                paramIndex = i;
                break;
            }
        }
        if (paramIndex == -1)
        {
            log.error("received a param resolution for a job that has no such parameter - ignored");
            return;
        }

        // Update the parameter with its value
        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            // TODO
            // resolvedJob.setParamValue(paramIndex, res);
            conn.commit();
        }

        // Perhaps launch the job
        if (resolvedJob.isReady(ctxMeta))
        {
            this.sendRunDescription(resolvedJob.getRD(ctxMeta), resolvedJob.getPlace(ctxMeta), resolvedJob, jmsSession);
        }
    }

    // Called within a JMS transaction - don't commit it.
    private void recvPJ(PipelineJob job, Session jmsSession) throws JMSException
    {
        PipelineJob j = job;
        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            j.insertOrUpdate(conn);
            j.getEnvValues(conn); // To load them, even if empty
        }

        // Check the job is OK
        EventSourceWrapper toRun;
        State s;
        try
        {
            toRun = j.getActive(ctxMeta);
            s = j.getState(ctxMeta);
        }
        catch (Exception e)
        {
            log.error("A pipeline job was received but was invalid - thrown out");
            return;
        }
        if (s == null)
        {
            log.error("A pipeline job was received but had no corresponding state in the current applications - thrown out");
            return;
        }

        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            j.setRunThis(toRun.getName());
            j.setBeganRunningAt(DateTime.now());
            j.insertOrUpdate(conn);
            conn.commit();
        }
        resolving.add(j);

        if (!toRun.isEnabled() || !s.isEnabled())
        {
            // Disabled => don't run it for real
            log.debug("Job execution request of a disabled element.");
            // recvRR(j.getDisabledResult());
        }
        else if (!this.e.isSimulator())
        {
            // Run - either sync or async.
            log.debug(String.format("Job execution request %s corresponds to an element (%s - %s) that should run async or sync", j.getId(),
                    toRun.getName(), toRun.getSourceClass()));
            RunResult res = toRun.run(new EngineCbRun(this.e, this.ctxMeta, j.getApplication(ctxMeta), j), j);

            if (res != null)
            {
                // Synchronous execution - go on to result analysis at once in the current thread.
                recvRR(res, jmsSession);
            }
            else
            {
                // Asynchronous execution - we need to wait for a RunResult in the JMS queue.
            }

            // TODO: parameters
        }
        else
        {
            // External active part, but simulation. Synchronously simulate it.
            log.debug(String.format("Job execution request %s will be simulated", j.getId()));
            try (Connection conn = this.ctxDb.getTransacDataSource().open())
            {
                recvRR(j.getSimulatedResult(conn), jmsSession);
            }
        }
    }

    private void recvRR(RunResult rr, Session jmsSession) throws JMSException
    {
        if (rr.outOfPlan)
        {
            log.info("An out of plan job run has just finished - it won't throw events");
        }
        if (rr.id1 == null)
        {
            // Means its a debug job - without PipelineJob (impossible in normal operations)
            log.warn("Test RR received");
            return;
        }
        log.info(String.format(String.format("Job %s has ended", rr.id1)));

        rr.logPath = FilenameUtils.concat(this.logDbPath, rr.logFileName);

        PipelineJob pj = null;
        for (PipelineJob pj2 : this.resolving)
        {
            if (pj2.getId().equals(rr.id1))
            {
                pj = pj2;
                break;
            }
        }
        if (pj == null)
        {
            log.error("A result was received that was not waited for - thrown out");
            return;
        }

        State s = null;
        Place p = null;
        Application a = null;
        if (!rr.outOfPlan)
        {
            s = pj.getState(ctxMeta);
            p = pj.getPlace(ctxMeta);
            a = pj.getApplication(ctxMeta);
            if (s == null)
            {
                log.error("A result was received for a pipeline job without state - thrown out");
                resolving.remove(pj);
                return;
            }
        }

        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            // Event throwing
            if (!rr.outOfPlan)
            {
                pj.getEnvValues(conn);
                Event e = pj.createEvent(rr, rr.end);
                SenderHelpers.sendEvent(e, this.jmsProducer, jmsSession, ctxMeta, true);
            }

            // Update the PJ (it will stay in the DB for a while)
            pj.setStatus("DONE");
            if (rr.start != null)
            {
                pj.setBeganRunningAt(rr.start);
            }
            pj.setStoppedRunningAt(rr.end);
            pj.setResultCode(rr.returnCode);
            pj.insertOrUpdate(conn);
            conn.commit();
        }

        // Send history
        SenderHelpers.sendHistory(pj.getEventLog(ctxMeta, rr), ctxMeta, this.jmsProducer, jmsSession, true,
                this.e.getLocalNode().getName());

        // Calendar progress
        if (!rr.outOfPlan && s.usesCalendar() && !pj.getIgnoreCalendarUpdating())
        {
            updateCalendar(pj, a, s, p);
        }

        // Free tokens
        if (!rr.outOfPlan && s.getTokens().size() > 0)
        {
            releaseTokens(s, pj, jmsSession);
        }

        // End
        resolving.remove(pj);
    }

    private void recvESRR(EventSourceRunResult esrr, Session jmsSession, UUID launchId) throws JMSException
    {
        PipelineJob pj = null;
        try (Connection conn = ctxDb.getTransacDataSource().beginTransaction())
        {
            pj = conn.createQuery("SELECT * FROM PipelineJob WHERE id=:id").addParameter("id", launchId)
                    .executeAndFetchFirst(PipelineJob.class);
        }
        this.recvRR(new RunResult(pj, esrr), jmsSession);
    }

    private void updateCalendar(PipelineJob pj, Application a, State s, Place p)
    {
        Calendar c = a.getCalendar(pj.getCalendarID());
        CalendarDay justDone = c.getDay(pj.getCalendarOccurrenceID());
        CalendarDay next = c.getOccurrenceAfter(justDone);

        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            CalendarPointer cp = s.getCurrentCalendarPointer(conn, p);

            cp.setLastEndedOccurrenceCd(justDone);
            cp.setRunning(false);
            if (pj.getResultCode() == 0)
            {
                cp.setLastEndedOkOccurrenceCd(justDone);
                cp.setNextRunOccurrenceCd(next);
            }
            cp.insertOrUpdate(conn);
            log.debug(String.format(
                    "At the end of the run, calendar status for state [%s] (chain [%s]) is Last: %s - LastOK: %s - LastStarted: %s - Next: %s - Latest failed: %s - Running: %s",
                    s.getRepresents().getName(), s.getContainerName(), cp.getLastEndedOccurrenceCd(ctxMeta).getValue(),
                    cp.getLastEndedOkOccurrenceCd(ctxMeta).getValue(), cp.getLastStartedOccurrenceCd(ctxMeta).getValue(),
                    cp.getNextRunOccurrenceCd(ctxMeta).getValue(), cp.getLatestFailed(), cp.getRunning()));
            conn.commit();
        }
    }

    private void releaseTokens(State s, PipelineJob pj, Session jmsSession) throws JMSException
    {
        for (Token tk : s.getTokens())
        {
            TokenRequest tr = new TokenRequest();
            tr.applicationID = pj.getAppID();
            tr.local = true;
            tr.placeID = pj.getPlaceID();
            tr.requestedAt = new DateTime();
            tr.requestingNodeID = this.e.getLocalNode().getComputingNode().getId();
            tr.stateID = pj.getStateID();
            tr.tokenID = tk.getId();
            tr.type = TokenRequestType.RELEASE;
            tr.pipelineJobID = pj.getId();

            SenderHelpers.sendTokenRequest(tr, ctxMeta, jmsSession, this.jmsProducer, true, this.e.getLocalNode().getBrokerName());
        }
    }

    public void sendRunDescription(RunDescription rd, Place p, PipelineJob pj, Session jmsSession) throws JMSException
    {
        // Always send to the node, not its hosting node.
        String qName = String.format(Constants.Q_RUNNER, p.getNode().getBrokerName());
        Destination destination = jmsSession.createQueue(qName);

        ObjectMessage m = jmsSession.createObjectMessage(rd);
        m.setJMSReplyTo(destEndJob);
        m.setJMSCorrelationID(pj.getId().toString());
        this.jmsProducer.send(destination, m);
        jmsSession.commit();
    }

    public void sendCalendarPointer(CalendarPointer cp, Calendar ca, Session jmsSession) throws JMSException
    {
        SenderHelpers.sendCalendarPointer(cp, ca, jmsSession, this.jmsProducer, true, this.ctxMeta.getEnvironment());
    }

    public void getParameterValue(RunDescription rd, PipelineJob pj, UUID paramId, Session jmsSession) throws JMSException
    {
        // Always send to the node, not its hosting node.
        Place p = pj.getPlace(ctxMeta);
        String qName = String.format(Constants.Q_RUNNER, p.getNode().getBrokerName());
        log.info(String.format("A command for parameter resolution will be sent for execution on queue %s", qName));
        Destination destination = jmsSession.createQueue(qName);

        ObjectMessage m = jmsSession.createObjectMessage(rd);
        m.setJMSReplyTo(destEndJob);
        m.setJMSCorrelationID(pj.getId() + "|" + paramId);
        this.jmsProducer.send(destination, m);
        jmsSession.commit();
    }

    public void sendParameterValue(String value, UUID paramID, PipelineJob pj, Session jmsSession) throws JMSException
    {
        // This is a loopback send (used by static parameter value mostly)
        log.debug(String.format("A param value resolved locally (static) will be sent to the local engine ( value is %s)", value));

        TextMessage m = jmsSession.createTextMessage(value);
        m.setJMSCorrelationID(pj.getId() + "|" + paramID.toString());
        this.jmsProducer.send(destEndJob, m);
        jmsSession.commit();
    }
}
