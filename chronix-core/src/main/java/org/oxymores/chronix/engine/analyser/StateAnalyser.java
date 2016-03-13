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
package org.oxymores.chronix.engine.analyser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jms.MessageProducer;
import javax.jms.Session;

import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * The heart of the event engine. One is created for each state/event couple. TODO: transform it to EventAnalyser
 */
public class StateAnalyser
{
    private static final Logger log = LoggerFactory.getLogger(StateAnalyser.class);

    /**
     * The State on which the analysis occurs (the target)
     */
    private State state;

    /**
     * The application scoping the analysis
     */
    private Application application;

    private Map<UUID, TransitionAnalyser> analysis = new HashMap<>();

    public List<Event> consumedEvents = new ArrayList<>();

    /**
     * 
     * @param s
     *            the state to analyse
     * @param conn
     *            an open SQL2O connection
     * @param evt
     *            the event that has triggered this analysis
     * @param application
     *            The application scoping the analysis
     */
    public StateAnalyser(Application application, State s, Event evt, Connection conn, MessageProducer pjProducer, Session session,
            ExecutionNode localNode)
    {
        this.state = s;
        this.application = application;

        // Get scope events
        List<Event> tmpEvents = conn.createQuery("SELECT e.* FROM Event e WHERE e.level0Id = :level0Id AND e.level1Id = :level1Id")
                .addParameter("level0Id", evt.getLevel0Id()) // $The unique ID associated to a chain run instance$
                .addParameter("level1Id", evt.getLevel1Id()) // $The chain ID$
                .executeAndFetch(Event.class);

        // Remove consumed events (first filter: those which are completely consumed)
        List<Event> scopeEvents = new ArrayList<>();
        for (Event e : tmpEvents)
        {
            for (Place p : s.getRunsOnPlaces())
            {
                if (!scopeEvents.contains(e) && !e.wasConsumedOnPlace(p, s, conn))
                {
                    scopeEvents.add(e);
                }
            }
        }

        // The current event may not yet be DB persisted
        if (!scopeEvents.contains(evt))
        {
            scopeEvents.add(evt);
        }

        log.debug("There are " + scopeEvents.size() + " events not consumed to be considered for analysis");

        // Check every incoming transition: are they allowed?
        for (DTOTransition tr : s.getTransitionsReceivedHere())
        {
            TransitionAnalyser tar = new TransitionAnalyser(application, tr, scopeEvents, conn);

            if (tar.blockedOnAllPlaces() && s.getRepresentsContainer().isAnd())
            {
                // No need to go further - at least one transition will block everything for all places
                log.debug(String.format("State %s (%s - chain %s) is NOT allowed to run due to transition from %s", s.getId(),
                        s.getRepresents().getName(), s.getContainerName(),
                        this.application.getState(tr.getFrom()).getRepresents().getName()));
                this.analysis.clear();
                return;
            }

            this.analysis.put(tr.getId(), tar);
        }

        List<Place> places = this.getPossiblePlaces();
        log.debug(String.format("According to transitions, the state [%s] in chain [%s] could run on %s places",
                s.getRepresents().getName(), s.getContainerName(), places.size()));

        // Check calendar
        for (Place p : places.toArray(new Place[0]))
        {
            if (!s.canRunAccordingToCalendarOnPlace(conn, p))
            {
                places.remove(p);
            }
        }
        log.debug(String.format("After taking calendar conditions into account, the state [%s] in chain [%s] could run on %s places",
                s.getRepresents().getName(), s.getContainerName(), places.size()));

        // Go
        if (!places.isEmpty())
        {
            log.debug(String.format(
                    "State (%s - chain %s) is triggered by the event on %s of its places. Analysis has consumed %s events on these places.",
                    s.getRepresents().getName(), s.getContainerName(), places.size(), this.consumedEvents.size()));

            this.consumeEvents(s, this.consumedEvents, places, conn);
            for (Place p : places)
            {
                if (p.getNode().getComputingNode() == localNode)
                {
                    s.runFromEngine(p, conn, pjProducer, session, evt);
                }
            }
        }
        else
        {
            this.analysis.clear();
        }
    }

    private List<Place> getPossiblePlaces()
    {
        ArrayList<Place> res = new ArrayList<>();
        places: for (Place p : state.getRunsOn().getPlaces())
        {
            Set<Event> ce = new HashSet<>();

            if (state.getRepresentsContainer().isAnd())
            {
                for (TransitionAnalyser tra : this.analysis.values())
                {
                    if (!tra.allowedOnPlace(p))
                    {
                        ce.clear();
                        continue places;
                    }
                    ce.addAll(tra.eventsConsumedOnPlace(p));
                }
                res.add(p);
            }

            if (state.getRepresentsContainer().isOr())
            {
                for (TransitionAnalyser tra : this.analysis.values())
                {
                    if (tra.allowedOnPlace(p))
                    {
                        ce.addAll(tra.eventsConsumedOnPlace(p));
                        res.add(p);
                        break;
                    }
                }
            }

            this.consumedEvents.addAll(ce);
        }

        return res;
    }

    private void consumeEvents(State s, List<Event> events, List<Place> places, Connection conn)
    {
        for (Event e : events)
        {
            for (Place p : places)
            {
                log.debug(String.format("Event %s marked as consumed on place %s", e.getId(), p.getName()));
                EventConsumption ec = new EventConsumption();
                ec.setEventID(e.getId());
                ec.setPlaceID(p.getId());
                ec.setStateID(s.getId());
                ec.setAppID(e.getAppID());

                ec.insert(conn);
            }
        }
    }
}
