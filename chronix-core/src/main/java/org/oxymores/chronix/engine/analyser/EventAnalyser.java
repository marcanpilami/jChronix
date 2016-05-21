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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jms.MessageProducer;
import javax.jms.Session;

import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * The heart of the event engine. One is created for each state/event couple. It will take into account all events that are in the same
 * scope as the event given to analyse.<br>
 * It actually delegates the analysis to subdivisions of the scope: this is the main class, then one analyser per transition, then reduced
 * scope, then per place.
 */
public class EventAnalyser
{
    private static final Logger log = LoggerFactory.getLogger(EventAnalyser.class);

    /**
     * The State on which the analysis occurs (the target)
     */
    private State toState;

    /**
     * The application scoping the analysis
     */
    private Application application;

    /**
     * Each transition is analysed separately.This stores the sub-analysers.
     */
    private Map<UUID, TransitionAnalyser> analysis = new HashMap<>();

    private List<Place> places;

    /**
     * Create a new analysis for the given event on the given state. Analysis proceeds immediately.
     * 
     * @param s
     *            the state to analyse (must be a potential client of the given event)
     * @param conn
     *            an open SQL2O connection
     * @param evt
     *            the event that has triggered this analysis
     * @param application
     *            The application scoping the analysis
     */
    public EventAnalyser(Application application, State s, Event evt, Connection conn, MessageProducer pjProducer, Session session,
            ExecutionNode localNode)
    {
        this.toState = s;
        this.application = application;

        // Get scope events
        List<Event> scopeEvents = conn.createQuery("SELECT e.* FROM Event e WHERE e.level0Id = :level0Id AND e.level1Id = :level1Id")
                .addParameter("level0Id", evt.getLevel0Id()) // $The unique ID associated to a chain run instance$
                .addParameter("level1Id", evt.getLevel1Id()) // $The chain ID$
                .executeAndFetch(Event.class);

        Collection<EventConsumption> consumption = conn
                .createQuery(
                        "SELECT c.* FROM EVENTCONSUMPTION c LEFT JOIN EVENT e ON c.eventID = e.id WHERE e.level0Id = :level0Id AND e.level1Id = :level1Id")
                .addParameter("level0Id", evt.getLevel0Id()).addParameter("level1Id", evt.getLevel1Id())
                .executeAndFetch(EventConsumption.class);

        // The current event may not yet be DB persisted
        scopeEvents.add(evt);

        log.debug("There are " + scopeEvents.size() + " events (potentially already consumed) to be considered for analysis");

        // Check every incoming transition: are they allowed?
        for (DTOTransition tr : s.getTransitionsReceivedHere())
        {
            TransitionAnalyser tar = new TransitionAnalyser(this.application, tr, scopeEvents, consumption, conn);
            this.analysis.put(tr.getId(), tar);
        }

        this.places = this.getPossiblePlaces();
        log.debug(String.format("According to transitions, the state [%s] in chain [%s] could run on %s places",
                s.getEventSourceDefinition().getName(), s.getContainerName(), places.size()));
        if (this.places.isEmpty())
        {
            return;
        }

        // Check calendar
        for (Place p : places.toArray(new Place[0]))
        {
            if (!s.canRunAccordingToCalendarOnPlace(conn, p))
            {
                places.remove(p);
            }
        }
        log.debug(String.format("After taking calendar conditions into account, the state [%s] in chain [%s] could run on %s places",
                s.getEventSourceDefinition().getName(), s.getContainerName(), places.size()));

        // Go
        if (!places.isEmpty())
        {
            log.debug(String.format("State (%s - chain %s) is triggered by the event on %s of its places.",
                    s.getEventSourceDefinition().getName(), s.getContainerName(), places.size()));

            this.consumeEvents(conn);
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
        places: for (Place p : toState.getRunsOn().getPlaces())
        {
            if (toState.getEventSourceDefinition().isAnd())
            {
                for (TransitionAnalyser tra : this.analysis.values())
                {
                    if (!tra.allowedOnPlace(p))
                    {
                        continue places;
                    }
                }
                res.add(p);
            }

            if (toState.getEventSourceDefinition().isOr())
            {
                for (TransitionAnalyser tra : this.analysis.values())
                {
                    if (tra.allowedOnPlace(p))
                    {
                        res.add(p);
                        break;
                    }
                }
            }
        }

        return res;
    }

    private void consumeEvents(Connection conn)
    {
        for (TransitionAnalyser an : this.analysis.values())
        {
            an.consumeEvents(conn, this.places);
        }
    }

    /**
     * All the events that were needed to create all the launch orders created by this analysis. May be empty if analysed event has no
     * consequences.
     */
    public Set<Event> getUsedEvents()
    {
        Set<Event> res = new HashSet<>();
        for (TransitionAnalyser an : this.analysis.values())
        {
            res.addAll(an.getUsedEvents());
        }
        return res;
    }
}
