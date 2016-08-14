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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.api.prm.AsyncParameterResult;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.FunctionalOccurrence;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.ParameterDef;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.app.Token;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.context.EngineCbRun;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.ParameterResolutionRequest;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.data.RunResult;
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

    private String logDbPath;
    private ChronixContextMeta ctxMeta;
    private ChronixContextTransient ctxDb;
    private ChronixEngine engine;

    // The list of jobs waiting for parameter resolution
    private Map<UUID, PipelineJob> resolvingPJ;

    // The list of dynamic parameters waiting for their own parameter resolutions
    private Map<UUID, ParameterResolutionRequest> resolvingPrm = new HashMap<>();

    private MessageProducer jmsProducer;

    RunnerManager(ChronixEngine e, ChronixContextMeta ctxMeta, ChronixContextTransient ctxDb, String logPath)
    {
        this.ctxDb = ctxDb;
        this.ctxMeta = ctxMeta;
        this.engine = e;

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
        resolvingPJ = new HashMap<>();
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
                else if (o instanceof AsyncParameterResult)
                {
                    AsyncParameterResult apr = (AsyncParameterResult) o;
                    recvAPR(apr, jmsSession);
                }
                else
                {
                    log.warn("An object was received by the Runner Manager that was not of a valid type. It will be ignored.");
                    return;
                }

            }
            catch (JMSException e)
            {
                throw new ListenerRollbackException(
                        "An error occurred during job reception. Message will stay in queue and will be analysed later", e);
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

    ///////////////////////////////////////////////////////////////////////////
    // Parameter resolution
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // New job handling
    ///////////////////////////////////////////////////////////////////////////

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
        EventSourceDef toRun;
        Application a;
        State s;
        Place p;
        try
        {
            a = j.getApplication(ctxMeta);
            toRun = j.getActive(ctxMeta);
            s = j.getState(ctxMeta);
            p = j.getPlace(ctxMeta);
        }
        catch (Exception e)
        {
            log.error("A pipeline job was received but was invalid - thrown out", e);
            return;
        }
        if (s == null || p == null)
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
        resolvingPJ.put(j.getId(), j);

        if (!toRun.isEnabled() || !s.isEnabled())
        {
            // Disabled => don't run it for real
            // TODO: call disabled methods
            log.debug("Job execution request of a disabled element.");
            // recvRR(j.getDisabledResult());
        }
        else if (!this.engine.isSimulator())
        {
            // Run - either sync or async.
            log.debug(String.format("Job execution request %s corresponds to an element (%s - %s) that should run async or sync", j.getId(),
                    toRun.getName(), toRun.getSourceTypeName()));

            // Parameter resolution
            if (!toRun.getAdditionalParameters().isEmpty() || !toRun.getFields().isEmpty())
            {
                // There are parameters to solve. In this case, actual run actually occurs at the end of all parameter resolutions.
                j.initParamResolution(toRun);

                for (ParameterDef h : toRun.getAllParameters())
                {
                    this.resolveParameter(h, j.getId(), null, engine.getLocalNode().getName(), a, jmsSession);
                }
            }
            else
            {
                // No parameters. In this case, direct run.
                launch(j, jmsSession);
            }
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

    private void resolveParameter(ParameterDef ph, UUID parentSourceLaunch, UUID parentParameterLaunch, String targetNodeName,
            Application a, Session jmsSession)
    {
        ParameterResolutionRequest rq = new ParameterResolutionRequest(ph,
                String.format(Constants.Q_RUNNERMGR, engine.getLocalNode().getName()), targetNodeName, parentSourceLaunch,
                parentParameterLaunch);
        this.resolvingPrm.put(rq.getRequestId(), rq);

        if (rq.isReference())
        {
            // Simple redirection
            resolveParameter(a.getSharedParameter(rq.getReference()), null, rq.getRequestId(), targetNodeName, a, jmsSession);
        }
        else if (rq.getDirectValue() != null)
        {
            // Simple value. Go directly to value analysis.
            AsyncParameterResult apr = new AsyncParameterResult();
            apr.requestId = rq.getRequestId();
            apr.result = rq.getDirectValue();
            apr.success = true;

            recvAPR(apr, jmsSession);
        }
        else if (rq.isDynamic())
        {
            if (rq.isReady())
            {
                // Parameter needs a resolution but is already ready for resolution (itself has no parameters)
                String newRes = rq.getParameterDef().getValue(rq);
                if (newRes != null)
                {
                    // Synchronous result - therefore continue synchronously.
                    AsyncParameterResult apr = new AsyncParameterResult();
                    apr.requestId = rq.getRequestId();
                    apr.result = newRes;
                    apr.success = true;
                    recvAPR(apr, jmsSession);
                }
            }
            else
            {
                // Parameter needs a resolution but first we need to resolve its own parameters
                for (ParameterDef childPh : rq.getParameterDef().getAllParameters())
                {
                    resolveParameter(childPh, null, rq.getRequestId(), targetNodeName, a, jmsSession);
                }
            }
        }

    }

    private void recvAPR(AsyncParameterResult res, Session jmsSession)
    {
        // Get the request corresponding to this result.
        ParameterResolutionRequest rq = this.resolvingPrm.get(res.requestId);
        if (rq == null)
        {
            log.warn("received an unexpected param resolution - ignored. Can happen on scheduler restart after a dirty shutdown.");
            return;
        }

        // Whatever happens, we are not waiting for a result for this request anymore.
        this.resolvingPrm.remove(res.requestId);

        // Result handling
        if (rq.getParentParameterRequest() != null)
        {
            // We have received the value of a parameter of a dynamic parameter
            ParameterResolutionRequest parentRequest = this.resolvingPrm.get(rq.getParentParameterRequest());
            if (parentRequest == null)
            {
                log.error("inconsistent parameter resolution requests. Plan may go awry.");
                return;
            }
            parentRequest.setFieldOrParamValue(res, rq);

            if (parentRequest.isReady())
            {
                String newRes = parentRequest.getParameterDef().getValue(parentRequest);
                if (newRes != null)
                {
                    // Synchronous result - therefore continue synchronously.
                    AsyncParameterResult apr = new AsyncParameterResult();
                    apr.requestId = parentRequest.getRequestId();
                    apr.result = newRes;
                    apr.success = true;
                    recvAPR(apr, jmsSession);
                }
            }
        }
        else if (rq.getParentSourceRequest() != null)
        {
            // We have received the value of a parameter of a pipeline job
            PipelineJob pj = this.resolvingPJ.get(rq.getParentSourceRequest());
            if (pj == null)
            {
                log.error("inconsistent parameter resolution requests. Plan may go awry.");
                return;
            }

            // Update the parameter with its value
            pj.setParamOrFieldValue(res, rq);

            // Perhaps launch the job
            if (pj.isReady(ctxMeta))
            {
                launch(pj, jmsSession);
            }
        }
        else
        {
            log.error("Invalid async parameter resolution result received (no parent). This may prevent the normal progress of the plan");
        }

    }

    private void launch(PipelineJob pj, Session jmsSession)
    {
        RunResult res = pj.getActive(ctxMeta).run(new EngineCbRun(this.engine, this.ctxMeta, pj.getApplication(ctxMeta), pj), pj);

        if (res != null)
        {
            // Synchronous execution - go on to result analysis at once in the current thread.
            try
            {
                recvRR(res, jmsSession);
            }
            catch (JMSException e)
            {
                throw new ListenerRollbackException("error during run result analysis", e);
            }
        }
        else
        {
            // Asynchronous execution - we need to wait for a RunResult in the JMS queue.
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Job result handling
    ///////////////////////////////////////////////////////////////////////////

    private void recvRR(RunResult rr, Session jmsSession) throws JMSException
    {
        if (rr.outOfPlan)
        {
            log.info("An out of plan job run has just finished - it won't throw events");
        }
        if (rr.launchId == null)
        {
            // Means its a debug job - without PipelineJob (impossible in normal operations)
            log.warn("Test RR received");
            return;
        }
        log.info(String.format(String.format("Job %s has ended", rr.launchId)));

        rr.logPath = FilenameUtils.concat(this.logDbPath, rr.logFileName);

        PipelineJob pj = this.resolvingPJ.get(rr.launchId);
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
        }
        if (s == null)
        {
            log.error("A result was received for a pipeline job without state - thrown out");
            resolvingPJ.remove(pj);
            return;
        }

        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            // Event throwing
            if (!rr.outOfPlan)
            {
                pj.getEnvValues(conn);
                Event e = pj.createEvent(rr, rr.end, this.ctxMeta);
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
                this.engine.getLocalNode().getName());

        // Calendar progress
        if (!rr.outOfPlan && s.usesCalendar() && !pj.getIgnoreCalendarUpdating())
        {
            // Pointer on state
            updateCalendar(pj, a, s, p);

            // Pointer on calendar itself
            if (s.isCalendarUpdater() && rr.success)
            {
                try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
                {
                    FunctionalSequence c = a.getCalendar(pj.getCalendarID());
                    c.advanceByOne(this.ctxMeta.getEnvironment(), jmsSession, conn, jmsProducer);
                }
            }
        }

        // Free tokens
        if (!rr.outOfPlan && !s.getTokens().isEmpty())
        {
            releaseTokens(s, pj, jmsSession);
        }

        // End
        resolvingPJ.remove(pj);
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
        FunctionalSequence c = a.getCalendar(pj.getCalendarID());
        FunctionalOccurrence justDone = c.getOccurrence(pj.getCalendarOccurrenceID());
        FunctionalOccurrence next = c.getOccurrenceAfter(justDone);

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
                    s.getEventSourceDefinition().getName(), s.getContainerName(), cp.getLastEndedOccurrenceCd(ctxMeta).getLabel(),
                    cp.getLastEndedOkOccurrenceCd(ctxMeta).getLabel(), cp.getLastStartedOccurrenceCd(ctxMeta).getLabel(),
                    cp.getNextRunOccurrenceCd(ctxMeta).getLabel(), cp.getLatestFailed(), cp.getRunning()));
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
            tr.requestingNodeID = this.engine.getLocalNode().getComputingNode().getId();
            tr.stateID = pj.getStateID();
            tr.tokenID = tk.getId();
            tr.type = TokenRequestType.RELEASE;
            tr.pipelineJobID = pj.getId();

            SenderHelpers.sendTokenRequest(tr, ctxMeta, jmsSession, this.jmsProducer, true, this.engine.getLocalNode().getBrokerName());
        }
    }

    public void sendCalendarPointer(CalendarPointer cp, FunctionalSequence ca, Session jmsSession) throws JMSException
    {
        SenderHelpers.sendCalendarPointer(cp, ca, jmsSession, this.jmsProducer, true, this.ctxMeta.getEnvironment());
    }
}
