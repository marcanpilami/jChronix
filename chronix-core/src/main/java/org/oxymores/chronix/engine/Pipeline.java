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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.context.Application2;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.engine.data.TokenRequest.TokenRequestType;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class Pipeline extends BaseListener implements Runnable
{
    private static final Logger log = LoggerFactory.getLogger(Pipeline.class);

    private MessageProducer jmsJRProducer;

    private LinkedBlockingQueue<PipelineJob> entering;
    private List<PipelineJob> waitingSequence;
    private List<PipelineJob> waitingToken;
    private List<PipelineJob> waitingTokenAnswer;
    private List<PipelineJob> waitingExclusion;
    private List<PipelineJob> waitingRun;

    private Boolean run = true;
    private Semaphore analyze;

    void startListening(Broker broker) throws JMSException
    {
        this.init(broker);

        this.analyze = new Semaphore(1);

        // Outgoing producer for job runner
        this.jmsJRProducer = this.jmsSession.createProducer(null);

        // Create analysis queues
        entering = new LinkedBlockingQueue<>();
        waitingSequence = new ArrayList<>();
        waitingToken = new ArrayList<>();
        waitingExclusion = new ArrayList<>();
        waitingRun = new ArrayList<>();
        waitingTokenAnswer = new ArrayList<>();

        // Retrieve jobs from previous service launches
        List<PipelineJob> old;
        try (Connection conn = ctxDb.getTransacDataSource().open())
        {
            old = conn.createQuery("SELECT * FROM PipelineJob j WHERE j.status = :status").addParameter("status", "CHECK_SYNC_CONDS")
                    .executeAndFetch(PipelineJob.class);
        }
        entering.addAll(old);

        // Register on incoming queue
        qName = String.format(Constants.Q_PJ, brokerName);
        this.subscribeTo(qName);

        // Start thread
        (new Thread(this)).start();
    }

    @Override
    void stopListening()
    {
        try
        {
            // wait for end of analysis
            this.analyze.acquire();
        }
        catch (InterruptedException e)
        {
            // Do nothing
        }
        super.stopListening();
        this.run = false;
        try
        {
            this.jmsJRProducer.close();
        }
        catch (JMSException e)
        {
            log.warn("An error occurred while closing the Pipleine. Not a real issue, but please report it", e);
        }
        this.analyze.release();
    }

    @Override
    public void run()
    {
        while (this.run)
        {
            // Poll for a job
            PipelineJob pj = null;
            try
            {
                pj = entering.poll(1, TimeUnit.MINUTES);
            }
            catch (InterruptedException e2)
            {
                // Normal
            }
            if (pj != null && pj.getAppID() != null)
            {
                Place p = null;
                org.oxymores.chronix.core.State s = null;
                Application2 a;
                try
                {
                    a = pj.getApplication(ctxMeta);
                    p = pj.getPlace(ctxMeta);
                    s = pj.getState(ctxMeta);
                }
                catch (Exception e)
                {
                    a = null;
                }
                if (s == null || p == null || a == null)
                {
                    log.error("A job was received in the pipeline without any corresponding local application data - ignored");
                }
                else
                {
                    waitingSequence.add(pj);
                    log.debug(String.format("A job was registered in the pipeline: %s", pj.getId()));
                }
            }

            // Synchro - helps to stop properly
            try
            {
                this.analyze.acquire();
            }
            catch (InterruptedException e1)
            {
                // Do nothing
            }
            if (!this.run)
            {
                break;
            }

            // On to analysis
            ArrayList<PipelineJob> toAnalyse = new ArrayList<>();

            toAnalyse.clear();
            toAnalyse.addAll(waitingSequence);
            for (PipelineJob j : toAnalyse)
            {
                anSequence(j);
            }

            toAnalyse.clear();
            toAnalyse.addAll(waitingToken);
            for (PipelineJob j : toAnalyse)
            {
                anToken(j);
            }

            toAnalyse.clear();
            toAnalyse.addAll(waitingExclusion);
            for (PipelineJob j : toAnalyse)
            {
                anExclusion(j);
            }

            toAnalyse.clear();
            toAnalyse.addAll(waitingRun);
            for (PipelineJob j : toAnalyse)
            {
                runPJ(j);
            }
            this.analyze.release();
        }
    }

    @Override
    public void onMessageAction(Message msg)
    {
        ObjectMessage omsg = (ObjectMessage) msg;
        PipelineJob pj = null;
        TokenRequest rq = null;
        try
        {
            Object o = omsg.getObject();
            if (o instanceof PipelineJob)
            {
                pj = (PipelineJob) o;
            }
            else if (o instanceof TokenRequest)
            {
                rq = (TokenRequest) o;
            }
            else
            {
                log.warn("An object was received on the pipeline queue but was not a job or a token request answer! Ignored.");
                jmsCommit();
                return;
            }

        }
        catch (JMSException e)
        {
            log.error("An error occurred during job reception. BAD. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        if (pj != null)
        {
            try (Connection conn = ctxDb.getTransacDataSource().beginTransaction())
            {
                // So that we find it again after a crash/stop
                pj.setStatus(Constants.JI_STATUS_CHECK_SYNC_CONDS);
                pj.setEnteredPipeAt(DateTime.now());
                pj.insertOrUpdate(conn);
                conn.commit();
            }
            log.debug(String.format("A job has entered the pipeline: %s", pj.getId()));
            jmsCommit();
            entering.add(pj);
        }

        if (rq != null)
        {
            log.debug("Token message received");
            try
            {
                this.analyze.acquire();
            }
            catch (InterruptedException e1)
            {
                // Not an issue
            }

            pj = null;
            for (PipelineJob jj : waitingTokenAnswer)
            {
                if (jj.getId().equals(rq.pipelineJobID))
                {
                    pj = jj;
                }
            }
            if (pj == null)
            {
                log.warn("A token was given to an unexisting PJ. Ignored");
                this.analyze.release();
                jmsCommit();
                return;
            }

            waitingTokenAnswer.remove(pj);
            waitingExclusion.add(pj);
            log.debug(String.format("Job %s has finished token analysis - on to exclusion analysis", pj.getId()));

            this.analyze.release();
            jmsCommit();
            entering.add(new PipelineJob()); // TODO: why?
        }
    }

    private void anSequence(PipelineJob pj)
    {
        // TODO: really check sequences
        waitingSequence.remove(pj);
        waitingToken.add(pj);
        log.debug(String.format("Job %s has finished sequence analysis - on to token analysis", pj.getId()));
    }

    private void anToken(PipelineJob pj)
    {
        org.oxymores.chronix.core.State s = pj.getState(ctxMeta);
        if (s.getTokens().size() > 0)
        {
            for (Token tk : s.getTokens())
            {
                TokenRequest tr = new TokenRequest();
                tr.applicationID = pj.getAppID();
                tr.local = true;
                tr.placeID = pj.getPlaceID();
                tr.requestedAt = new DateTime();
                tr.requestingNodeID = this.broker.getEngine().getLocalNode().getComputingNode().getId();
                tr.stateID = pj.getStateID();
                tr.tokenID = tk.getId();
                tr.type = TokenRequestType.REQUEST;
                tr.pipelineJobID = pj.getId();

                try
                {
                    SenderHelpers.sendTokenRequest(tr, ctxMeta, jmsSession, jmsJRProducer, true, this.brokerName);
                }
                catch (JMSException e)
                {
                    log.error("Could not send a token request. This will likely block the plan.", e);
                    jmsRollback();
                }

                waitingToken.remove(pj);
                waitingTokenAnswer.add(pj);
            }
        }
        else
        {
            // No tokens to analyse - continue advancing in the pipeline
            waitingToken.remove(pj);
            waitingExclusion.add(pj);
            log.debug(String.format("Job %s has finished token analysis (no tokens!) - on to exclusion analysis", pj.getId()));
        }
    }

    private void anExclusion(PipelineJob pj)
    {
        // TODO: really check exclusions
        waitingExclusion.remove(pj);
        waitingRun.add(pj);
        log.debug(String.format("Job %s has finished exclusion analysis - on to run", pj.getId()));
    }

    private void runPJ(PipelineJob pj)
    {
        // Remove from queue
        waitingRun.remove(pj);

        String qName = String.format(Constants.Q_RUNNERMGR, pj.getPlace(ctxMeta).getNode().getComputingNode().getBrokerName());
        try
        {
            Destination d = jmsSession.createQueue(qName);
            ObjectMessage om = jmsSession.createObjectMessage(pj);
            jmsJRProducer.send(d, om);
        }
        catch (JMSException e1)
        {
            log.error("Could not launch a job! Probable bug. Job request will be ignored to allow resuming the scheduler", e1);
            jmsCommit();
            return;
        }

        pj.setStatus(Constants.JI_STATUS_RUNNING);
        pj.setMarkedForRunAt(DateTime.now());
        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            pj.insertOrUpdate(conn);
            conn.commit();
        }

        jmsCommit();
        log.debug(String.format("Job %s was given to the runner queue %s", pj.getId(), qName));
    }
}
