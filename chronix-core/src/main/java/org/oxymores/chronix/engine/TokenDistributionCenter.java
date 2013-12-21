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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.transactional.TokenReservation;
import org.oxymores.chronix.engine.data.TokenRequest;
import org.oxymores.chronix.engine.data.TokenRequest.TokenRequestType;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

class TokenDistributionCenter extends BaseListener implements Runnable
{
    private static Logger log = Logger.getLogger(TokenDistributionCenter.class);

    private MessageProducer jmsProducer;

    private boolean running = true;
    private Semaphore mainLoop, localResource;
    private List<TokenReservation> shouldRenew;

    void startListening(Broker broker) throws JMSException
    {
        this.init(broker, true, false);
        log.debug(String.format("(%s) Initializing TokenDistributionCenter", ctx.getContextRoot()));

        // Sync
        mainLoop = new Semaphore(0);
        localResource = new Semaphore(1);
        this.shouldRenew = new ArrayList<TokenReservation>();

        // Register current object as a listener on ORDER queue
        qName = String.format(Constants.Q_TOKEN, brokerName);
        this.subscribeTo(qName);
        this.jmsProducer = this.jmsSession.createProducer(null);

        // Start thread
        new Thread(this).start();
    }

    @Override
    void stopListening()
    {
        running = false;
        mainLoop.release();
        try
        {
            localResource.acquire();
        }
        catch (InterruptedException e)
        {
            // Don't care
            log.debug("interruption");
        }
        super.stopListening();
    }

    @Override
    public void onMessage(Message msg)
    {
        ObjectMessage omsg = (ObjectMessage) msg;
        TokenRequest request;
        try
        {
            Object o = omsg.getObject();
            if (o instanceof TokenRequest)
            {
                request = (TokenRequest) o;
            }
            else
            {
                log.warn("An object was received on the token queue but was not a token request or token request ack! Ignored.");
                jmsCommit();
                return;
            }
        }
        catch (JMSException e)
        {
            log.error("An error occurred during token reception. BAD. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        // Check.
        Application a = ctx.getApplication(request.applicationID);
        if (a == null)
        {
            log.warn("A token for an application that does not run locally was received. Ignored.");
            jmsCommit();
            return;
        }

        Token tk = a.getToken(request.tokenID);
        Place p = a.getPlace(request.placeID);
        org.oxymores.chronix.core.State s = a.getState(request.stateID);
        ExecutionNode en = a.getNode(request.requestingNodeID);

        log.debug(String.format("Received a %s token request type %s on %s for state %s (application %s) for node %s. Local: %s",
                request.type, tk.getName(), p.getName(), s.getRepresents().getName(), a.getName(), en.getBrokerName(), request.local));

        // Case 1: TDC proxy: local request.
        if (request.local && request.type == TokenRequestType.REQUEST)
        {
            // Should be stored locally, so as to refresh the request every 5 minutes
            TokenReservation tr = new TokenReservation();
            tr.setApplicationId(request.applicationID.toString());
            tr.setGrantedOn(new Date());
            tr.setLocalRenew(true);
            tr.setPlaceId(request.placeID.toString());
            tr.setRenewedOn(tr.getGrantedOn());
            tr.setRequestedOn(tr.getGrantedOn());
            tr.setStateId(request.stateID.toString());
            tr.setTokenId(request.tokenID.toString());
            tr.setPending(true);
            tr.setRequestedBy(request.requestingNodeID.toString());

            trTransac.begin();
            emTransac.persist(tr);
            trTransac.commit();
            shouldRenew.add(tr);

            // Request should be sent to the node responsible for the distribution of this token
            request.local = false;
            try
            {
                SenderHelpers.sendTokenRequest(request, ctx, jmsSession, jmsProducer, false);
            }
            catch (JMSException e)
            {
                log.error(e.getMessage(), e);
                jmsRollback();
                return;
            }
            request.local = true;
        }

        // Case 2: TDC proxy: local release
        if (request.local && request.type == TokenRequestType.RELEASE)
        {
            // Delete the local element
            TokenReservation toRemove = null;
            for (TokenReservation tr : shouldRenew)
            {
                if (tr.getStateId().equals(request.stateID.toString()) && tr.getPlaceId().equals(request.placeID.toString()))
                {
                    toRemove = tr;
                }
            }
            if (toRemove != null)
            {
                shouldRenew.remove(toRemove);
            }

            // Request should be sent to the node responsible for the distribution of this token
            request.local = false;
            try
            {
                SenderHelpers.sendTokenRequest(request, ctx, jmsSession, jmsProducer, false);
            }
            catch (JMSException e)
            {
                log.error(e.getMessage(), e);
                jmsRollback();
                return;
            }
            request.local = true;
        }

        // Case 3: TDC: request
        if (!request.local && request.type == TokenRequestType.REQUEST)
        {
            trTransac.begin();
            log.debug(String.format("Analysing token request for PJ %s", request.pipelineJobID));
            processRequest(request);
            trTransac.commit();
        }

        // Case 4: TDC: release
        if (!request.local && request.type == TokenRequestType.RELEASE)
        {
            // Data

            // Log
            log.info(String.format("A token %s that was granted on %s (application %s) to node %s on state %s is released", tk.getName(),
                    p.getName(), a.getName(), en.getBrokerName(), s.getRepresents().getName()));

            // Find the element
            TokenReservation tr = getTR(request);

            trTransac.begin();
            emTransac.remove(tr);
            trTransac.commit();
        }

        // Case 5: TDC: RENEW
        if (!request.local && request.type == TokenRequestType.RENEW)
        {
            // Log
            log.debug(String.format("A token %s that was granted on %s (application %s) to node %s on state %s is renewed", tk.getName(),
                    p.getName(), a.getName(), en.getBrokerName(), s.getRepresents().getName()));

            // Find the element
            TokenReservation tr = getTR(request);

            trTransac.begin();
            tr.setRenewedOn(new Date());
            trTransac.commit();
        }

        // Case 6: TDC proxy: AGREE
        if (!request.local && request.type == TokenRequestType.AGREE)
        {
            // Just forward it to the pipeline
            ObjectMessage response;
            try
            {
                response = jmsSession.createObjectMessage(request);
                String qName = String.format(Constants.Q_PJ, a.getLocalNode().getHost().getBrokerName());
                Destination d = jmsSession.createQueue(qName);
                log.debug(String.format("A message will be sent to queue %s", qName));
                jmsProducer.send(d, response);
            }
            catch (JMSException e)
            {
                log.error(e.getMessage(), e);
                jmsRollback();
                return;
            }
        }

        // TDC: Step 1: Purge granted requests that are too old
        if (!request.local)
        {
            trTransac.begin();
            TypedQuery<TokenReservation> q = emTransac.createQuery(
                    "SELECT q from TokenReservation q where q.renewedOn < ?1 AND q.pending = FALSE", TokenReservation.class);
            q.setParameter(1, DateTime.now().minusMinutes(Constants.MAX_TOKEN_VALIDITY_MN).toDate());
            for (TokenReservation tr : q.getResultList())
            {
                Application aa = ctx.getApplication(tr.getApplicationId());
                org.oxymores.chronix.core.State ss = aa.getState(UUID.fromString(tr.getStateId()));
                Place pp = aa.getPlace(UUID.fromString(tr.getPlaceId()));
                ExecutionNode enn = aa.getNode(UUID.fromString(tr.getRequestedBy()));
                log.info(String
                        .format("A token that was granted on %s (application %s) to node %s on state %s will be revoked as the request was not renewed in the last 10 minutes",
                                pp.getName(), aa.getName(), enn.getBrokerName(), ss.getRepresents().getName()));

                // Remove from database
                emTransac.remove(tr);
            }
        }

        // TDC: Step 2: Now that the purge is done, analyse pending requests again - tokens may have freed
        if (!request.local)
        {
            TypedQuery<TokenReservation> q = emTransac.createQuery(
                    "SELECT q from TokenReservation q where q.pending = TRUE AND q.localRenew = FALSE", TokenReservation.class);
            for (TokenReservation tr : q.getResultList())
            {
                log.debug(String.format("Re-analysing token request for PJ %s", tr.getPipelineJobId()));
                processRequest(tr);
            }
            trTransac.commit();
        }

        // The end: commit JMS
        jmsCommit();
        log.debug("Commit successful");
    }

    private TokenReservation getTR(TokenRequest request)
    {
        TypedQuery<TokenReservation> q = emTransac.createQuery(
                "SELECT q from TokenReservation q where q.pipelineJobId = ?1 AND q.localRenew = FALSE", TokenReservation.class);
        q.setParameter(1, request.pipelineJobID.toString());
        return q.getSingleResult();
    }

    private void processRequest(TokenRequest request)
    {
        // Get data
        Application a = ctx.getApplication(request.applicationID);
        Token tk = a.getToken(request.tokenID);
        Place p = a.getPlace(request.placeID);
        org.oxymores.chronix.core.State s = a.getState(request.stateID);

        // Process
        processRequest(a, tk, p, request.requestedAt, s, null, request.pipelineJobID.toString(), request.requestingNodeID.toString());
    }

    private void processRequest(TokenReservation tr)
    {
        // Get data
        Application a = ctx.getApplication(tr.getApplicationId());
        Token tk = a.getToken(UUID.fromString(tr.getTokenId()));
        Place p = a.getPlace(UUID.fromString(tr.getPlaceId()));
        org.oxymores.chronix.core.State s = a.getState(UUID.fromString(tr.getStateId()));

        // Process
        processRequest(a, tk, p, new DateTime(tr.getRequestedOn()), s, tr, tr.getPipelineJobId(), tr.getRequestedBy());
    }

    private void processRequest(Application a, Token tk, Place p, DateTime requestedOn, org.oxymores.chronix.core.State s,
            TokenReservation existing, String pipelineJobId, String requestingNodeId)
    {
        // Locate all the currently allocated tokens on this Token/Place
        TypedQuery<TokenReservation> q = null;
        if (tk.isByPlace())
        {
            q = emTransac
                    .createQuery(
                            "SELECT q from TokenReservation q where q.tokenId = :tokenId AND q.renewedOn > :ro AND q.placeId = :placeId AND q.pending = FALSE AND q.localRenew = FALSE",
                            TokenReservation.class);
            q.setParameter("tokenId", tk.getId().toString());
            q.setParameter("ro", DateTime.now().minusMinutes(Constants.MAX_TOKEN_VALIDITY_MN).toDate());
            q.setParameter("placeId", p.getId().toString());
        }
        else
        {
            q = emTransac
                    .createQuery(
                            "SELECT q from TokenReservation q where q.tokenId = ?1 AND q.renewedOn > ?2 AND q.pending = FALSE AND q.localRenew = FALSE",
                            TokenReservation.class);
            q.setParameter(1, tk.getId().toString());
            q.setParameter(2, DateTime.now().minusMinutes(Constants.MAX_TOKEN_VALIDITY_MN).toDate());
        }
        List<TokenReservation> res = q.getResultList();

        if (res.size() >= tk.getCount())
        {
            // No available token
            log.warn(String.format("A token was requested but there are none available. (max is %s, allocated is %s)", tk.getCount(),
                    res.size()));

            // Store the request if not done already
            if (existing == null)
            {
                TokenReservation trs = new TokenReservation();
                trs.setGrantedOn(null);
                trs.setLocalRenew(false);
                // PENDING means not given yet
                trs.setPending(true);
                trs.setPlaceId(p.getId().toString());
                trs.setRenewedOn(null);
                trs.setRequestedOn(requestedOn.toDate());
                trs.setStateId(s.getId().toString());
                trs.setTokenId(tk.getId().toString());
                trs.setPipelineJobId(pipelineJobId);
                trs.setApplicationId(a.getId().toString());
                trs.setRequestedBy(requestingNodeId);

                emTransac.persist(trs);
            }
            else
            {
                existing.setRenewedOn(new Date());
            }

            // Don't send an answer to the caller.
        }
        else
        {
            // Available token
            log.debug(String.format("A token was requested and one can be issued (%s taken out of %s)", res.size(), tk.getCount()));

            TokenRequest answer = null;
            if (existing == null)
            {
                TokenReservation trs = new TokenReservation();
                trs.setGrantedOn(new Date());
                trs.setLocalRenew(false);
                trs.setPending(false);
                trs.setPlaceId(p.getId().toString());
                trs.setRenewedOn(trs.getGrantedOn());
                trs.setRequestedOn(requestedOn.toDate());
                trs.setStateId(s.getId().toString());
                trs.setTokenId(tk.getId().toString());
                trs.setPipelineJobId(pipelineJobId);
                trs.setApplicationId(a.getId().toString());
                trs.setRequestedBy(requestingNodeId);

                emTransac.persist(trs);
                answer = trs.getAgreeRequest();
            }
            else
            {
                existing.setPending(false);
                existing.setGrantedOn(new Date());
                existing.setRenewedOn(existing.getGrantedOn());
                answer = existing.getAgreeRequest();
            }

            // Send an answer
            ObjectMessage response;
            try
            {
                response = jmsSession.createObjectMessage(answer);
                Destination d = jmsSession.createQueue(String.format(Constants.Q_TOKEN, p.getNode().getHost().getBrokerName()));
                jmsProducer.send(d, response);
            }
            catch (JMSException e)
            {
                log.error(e.getMessage(), e);
                jmsRollback();
                return;
            }
        }
    }

    @Override
    public void run()
    {
        do
        {
            DateTime now = DateTime.now();

            // Sync
            try
            {
                localResource.acquire();
            }
            catch (InterruptedException e1)
            {
                // Don't care here, we can loop as we want.
                log.debug("interrupted, no worries");
            }

            // Renew token location for all boring
            for (TokenReservation tr : shouldRenew)
            {
                if (now.minusMinutes(Constants.TOKEN_RENEWAL_MN).compareTo(new DateTime(tr.getRenewedOn())) <= 0)
                {
                    try
                    {
                        log.debug("Sending a renewal request for token state");
                        SenderHelpers.sendTokenRequest(tr.getRenewalRequest(), ctx, jmsSession, jmsProducer, false);
                    }
                    catch (JMSException e)
                    {
                        log.error("Could not send a token renewal request. Will retry soon.", e);
                    }
                }
            }

            jmsCommit();

            // Sync
            localResource.release();

            try
            {
                // Loop every minute or so. No need to be precise here.
                if (mainLoop.tryAcquire(Constants.TOKEN_AUTO_RENEWAL_LOOP_PERIOD_S, TimeUnit.SECONDS))
                {
                    log.trace("TokenDistributionCenter will loop because engine is stopping");
                }
            }
            catch (InterruptedException e)
            {
                // Once again, who cares?
                log.debug("interrupted");
            }
        } while (running);
    }
}
