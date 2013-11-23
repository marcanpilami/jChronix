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
import java.util.HashSet;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.Event;

class EventListener extends BaseListener
{
    private static Logger log = Logger.getLogger(EventListener.class);

    private MessageProducer producerPJ;

    void startListening(Broker broker) throws JMSException
    {
        this.init(broker, true, false);
        log.info(String.format("(%s) Starting an event engine", ctx.configurationDirectoryPath));

        this.qName = String.format("Q.%s.EVENT", brokerName);
        this.subscribeTo(qName);

        this.producerPJ = this.jmsSession.createProducer(null);
    }

    @Override
    public void onMessage(Message msg)
    {
        // For commits: remember an event can be analyzed multiple times
        // without problems.
        ObjectMessage omsg = (ObjectMessage) msg;
        Event evt, tmp;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Event))
            {
                log.warn("An object was received on the event queue but was not an event! Ignored.");
                jmsSession.commit();
                return;
            }
            evt = (Event) o;
            tmp = this.emTransac.find(Event.class, evt.getId());
            if (tmp != null)
            {
                evt = tmp;
            }
        }
        catch (JMSException e)
        {
            log.error("An error occurred during event reception. BAD. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        //
        // Check event is OK while getting data from event
        Application a = null;
        State s = null;
        ActiveNodeBase active = null;
        try
        {
            a = evt.getApplication(ctx);
            s = evt.getState(ctx);
            active = s.getRepresents();
        }
        catch (Exception e)
        {
            log.error("An event was received that was not related to a local applicaiton. Discarded.");
            jmsCommit();
            return;
        }
        log.debug(String.format("Event %s (from application %s / active node %s) was received and will be analysed", evt.getId(),
                a.getName(), active.getName()));

        //
        // Analyse event!
        trTransac.begin();

        // Should it be discarded?
        if (evt.getBestBefore() != null && evt.getBestBefore().before(new Date()))
        {
            log.info(String
                    .format("Event %s (from application %s / active node %s) was discarded because it was too old according to its 'best before' date",
                            evt.getId(), a.getName(), active.getName()));
            jmsCommit();
            return;
        }

        // All clients
        ArrayList<State> clientStates = s.getClientStates();

        // All client physical nodes
        ArrayList<ExecutionNode> clientPN = new ArrayList<ExecutionNode>();
        for (State st : clientStates)
        {
            for (ExecutionNode en : st.getRunsOnPhysicalNodes())
            {
                if (!clientPN.contains(en))
                {
                    clientPN.add(en);
                }
            }
        }

        // All local clients
        ArrayList<State> localConsumers = new ArrayList<State>();
        for (State st : clientStates)
        {
            if (st.getApplication().equals(a))
            {
                localConsumers.add(st);
            }
        }

        // Analyze on every local consumer
        ArrayList<Event> toCheck = new ArrayList<Event>();
        for (State st : localConsumers)
        {
            toCheck.addAll(st.getRepresents().isStateExecutionAllowed(st, evt, emTransac, producerPJ, jmsSession, ctx).consumedEvents);
        }

        // Ack
        log.debug(String.format("Event id %s was received, analysed and will now be acked in the JMS queue", evt.getId()));
        if (tmp == null)
        {
            emTransac.persist(evt);
        }
        trTransac.commit();
        jmsCommit();
        log.debug(String.format("Event id %s was received, analysed and acked all right", evt.getId()));

        // Purge
        trTransac.begin();
        this.cleanUp(toCheck, emTransac);
        trTransac.commit();
    }

    private void cleanUp(List<Event> events, EntityManager em)
    {
        HashSet<Event> hs = new HashSet<Event>();
        hs.addAll(events);
        events.clear();
        events.addAll(hs);
        for (Event e : events)
        {
            boolean shouldPurge = true;
            State s = e.getState(ctx);
            ArrayList<State> clientStates = s.getClientStates();

            for (State cs : clientStates)
            {
                for (Place p : cs.getRunsOn().getPlaces())
                {
                    // Don't purge if place is local & event no consumed on that place
                    if (p.getNode().getHost() == e.getApplication(ctx).getLocalNode() && !e.wasConsumedOnPlace(p, cs))
                    {
                        shouldPurge = false;
                        break;
                    }
                    // If here, 'shouldPurge' stays at true
                }
            }

            if (shouldPurge)
            {
                em.remove(e);
                log.debug(String.format("Event %s will be purged", e.getId()));
            }
        }
    }
}
