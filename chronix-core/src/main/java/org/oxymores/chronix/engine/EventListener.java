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
import java.util.HashSet;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.engine.analyser.StateAnalyser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;

class EventListener implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private ChronixContextMeta ctxMeta;
    private ChronixContextTransient ctxDb;
    private ExecutionNode localNode;

    EventListener(ChronixContextMeta ctx1, ChronixContextTransient ctx2, ExecutionNode localNode)
    {
        this.ctxDb = ctx2;
        this.ctxMeta = ctx1;
        this.localNode = localNode;
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        // For commits: remember an event can be analyzed multiple times without problems.
        ObjectMessage omsg = (ObjectMessage) msg;
        Event evt;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Event))
            {
                log.warn("An object was received on the event queue but was not an event! Ignored.");
                return;
            }
            evt = (Event) o;
        }
        catch (JMSException e)
        {
            throw new ListenerRollbackException(
                    "An error occurred during event reception. BAD. Message will stay in queue and will be analysed later", e);
        }

        //
        // Check event is OK while getting data from event
        Application a;
        State s;
        EventSourceWrapper active;
        try
        {
            a = evt.getApplication(ctxMeta);
            s = evt.getState(ctxMeta);
            active = a.getEventSourceContainer(evt.getActiveID());
        }
        catch (Exception e)
        {
            log.error("An event was received that was not related to a local application. Discarded.");
            return;
        }
        log.debug(String.format("Event %s (from application %s / active node %s) was received and will be analysed", evt.getId(),
                a.getName(), active.getName()));

        //
        // Analyse event!
        ArrayList<Event> toCheck = new ArrayList<>();
        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            // Should it be discarded?
            if (evt.getBestBefore() != null && evt.getBestBefore().isBeforeNow())
            {
                log.info(String.format(
                        "Event %s (from application %s / active node %s) was discarded because it was too old according to its 'best before' date",
                        evt.getId(), a.getName(), active.getName()));
                return;
            }

            // All clients
            List<State> clientStates = s.getClientStates();

            // All local clients
            ArrayList<State> localConsumers = new ArrayList<>();
            for (State st : clientStates)
            {
                if (st.getRunsOnPhysicalNodes().contains(this.localNode))
                {
                    localConsumers.add(st);
                }
            }

            // Analyse on every local consumer
            for (State st : localConsumers)
            {
                StateAnalyser ana = new StateAnalyser(a, st, evt, conn, jmsProducer, jmsSession, localNode);
                toCheck.addAll(ana.consumedEvents);
            }

            // Ack
            log.debug(String.format("Event id %s was received, analysed and will now be acked in the JMS queue", evt.getId()));

            evt.insertOrUpdate(conn);
            conn.commit();
            // jmsCommit done in handler
            log.debug(String.format("Event id %s was received, analysed and acked all right", evt.getId()));
        }

        // Purge
        this.cleanUp(toCheck);
    }

    private void cleanUp(List<Event> events)
    {
        try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
        {
            Query q = conn.createQuery("DELETE FROM Event WHERE id=:id");
            HashSet<Event> hs = new HashSet<>();
            hs.addAll(events);
            events.clear();
            events.addAll(hs);
            int i = 0;
            for (Event e : events)
            {
                boolean shouldPurge = true;
                State s = e.getState(ctxMeta);
                List<State> clientStates = s.getClientStates();

                for (State cs : clientStates)
                {
                    for (Place p : cs.getRunsOn().getPlaces())
                    {
                        // Don't purge if place is local & event no consumed on that place
                        if (p.getNode().getComputingNode() == this.localNode && !e.wasConsumedOnPlace(p, cs, conn))
                        {
                            shouldPurge = false;
                            break;
                        }
                        // If here, 'shouldPurge' stays at true
                    }
                }

                if (shouldPurge)
                {
                    q.addParameter("id", e.getId()).addToBatch();
                    i++;
                    log.debug(String.format("Event %s will be purged", e.getId()));
                }
            }
            if (i > 0)
            {
                q.executeBatch();
            }
            conn.commit();
        }
    }
}
