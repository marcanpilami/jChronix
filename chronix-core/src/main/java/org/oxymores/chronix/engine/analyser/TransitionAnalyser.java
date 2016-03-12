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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.transactional.Event;
import org.sql2o.Connection;

class TransitionAnalyser
{
    // PlaceID, result.
    private Map<UUID, TransitionOnSinglePlaceAnalyser> placeAnalysises = new HashMap<UUID, TransitionOnSinglePlaceAnalyser>();

    private List<Place> analysisPlaces;

    private boolean parallelAnalysis;

    TransitionAnalyser(Application app, DTOTransition tr, List<Event> events, Connection conn)
    {
        parallelAnalysis = isTransitionParallelEnabled(app, tr);

        State from = app.getState(tr.getFrom());
        State to = app.getState(tr.getTo());

        this.analysisPlaces = from.getRunsOnPlaces();
        for (Place p : analysisPlaces)
        {
            // All events that are not consumed on this specific Place
            ArrayList<Event> virginEvents = new ArrayList<>();
            for (Event e : events)
            {
                if (!e.wasConsumedOnPlace(p, to, conn) && !virginEvents.contains(e))
                {
                    virginEvents.add(e);
                }
            }

            // Analyse
            TransitionOnSinglePlaceAnalyser analysis = new TransitionOnSinglePlaceAnalyser(app, tr, from.getRepresentsContainer(),
                    virginEvents, p, conn);

            if (!parallelAnalysis && !analysis.allowed)
            {
                // We already know the transition is KO, so no need to continue. Say the transition should block on all Places.
                placeAnalysises.clear();
                return;
            }

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

    List<Event> getConsumedEvents()
    {
        ArrayList<Event> res = new ArrayList<Event>();
        for (TransitionOnSinglePlaceAnalyser par : this.placeAnalysises.values())
        {
            if (!par.allowed)
            {
                continue;
            }
            for (Event e : par.consumedEvents)
            {
                if (!res.contains(e))
                {
                    res.add(e);
                }
            }
        }
        return res;
    }

    private boolean allowedOnAllPlaces()
    {
        boolean res = true;
        for (TransitionOnSinglePlaceAnalyser par : this.placeAnalysises.values())
        {
            res = res && par.allowed;
        }
        return res;
    }

    /**
     * Returns true if the transition is possible on at least one place.
     */
    boolean totallyBlocking()
    {
        for (TransitionOnSinglePlaceAnalyser par : this.placeAnalysises.values())
        {
            if (par.allowed)
            {
                return false;
            }
        }
        return true;
    }

    boolean allowedOnPlace(Place p)
    {
        if ((!this.parallelAnalysis && this.allowedOnAllPlaces()) || (this.placeAnalysises.get(p.getId()).allowed && this.parallelAnalysis))
        {
            return true;
        }
        return false;
    }

    List<Event> eventsConsumedOnPlace(Place p)
    {
        if (!this.parallelAnalysis && this.allowedOnAllPlaces())
        {
            return this.getConsumedEvents();
        }
        if (this.parallelAnalysis && this.placeAnalysises.get(p.getId()).allowed)
        {
            return this.placeAnalysises.get(p.getId()).consumedEvents;
        }

        return new ArrayList<Event>();
    }
}
