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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class TransitionAnalyser
{
    private static final Logger log = LoggerFactory.getLogger(TransitionAnalyser.class);

    // PlaceID, result. (one entry per target place)
    private Map<UUID, TransitionScopedAnalyser> placeAnalysises = new HashMap<>();

    // Target places
    private List<Place> analysisPlaces;

    private boolean parallelAnalysis;

    TransitionAnalyser(Application app, DTOTransition tr, Collection<Event> events, Collection<EventConsumption> consumedEvents,
            Connection conn)
    {
        parallelAnalysis = isTransitionParallelEnabled(app, tr);

        State from = app.getState(tr.getFrom());
        State to = app.getState(tr.getTo());

        log.debug(String.format("Transition from State %s (%s) to State %s (%s - chain %s) analysis with %s events", from.getId(),
                from.getEventSourceDefinition().getName(), to.getId(), to.getEventSourceDefinition().getName(), to.getContainerName(),
                events.size()));

        this.analysisPlaces = to.getRunsOnPlaces();

        for (Place p : analysisPlaces)
        {
            // Analyse
            TransitionScopedAnalyser analysis = new TransitionScopedAnalyser(tr, to, from, p, events, consumedEvents, parallelAnalysis,
                    conn);

            this.placeAnalysises.put(p.getId(), analysis);
        }
    }

    private Boolean isTransitionParallelEnabled(Application app, DTOTransition tr)
    {
        State from = app.getState(tr.getFrom());
        State to = app.getState(tr.getTo());

        if (!from.isParallel() || !to.isParallel())
        {
            return false;
        }

        if (!from.getRunsOn().equals(to.getRunsOn()))
        {
            return false;
        }

        return true;
    }

    boolean allowedOnPlace(Place p)
    {
        return this.placeAnalysises.get(p.getId()) != null ? this.placeAnalysises.get(p.getId()).allowed : false;
    }

    void consumeEvents(Connection conn, Collection<Place> restrictedToTargetPlaces)
    {
        for (TransitionScopedAnalyser an : this.placeAnalysises.values())
        {
            if (restrictedToTargetPlaces.contains(an.targetPlace))
            {
                an.consumeEvents(conn);
            }
        }
    }

    Set<Event> getUsedEvents()
    {
        Set<Event> res = new HashSet<>();
        for (TransitionScopedAnalyser an : this.placeAnalysises.values())
        {
            res.addAll(an.getUsedEvents());
        }
        return res;
    }
}
