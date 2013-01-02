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
import java.util.HashMap;
import java.util.UUID;

import org.oxymores.chronix.core.MultipleTransitionsHandlingMode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.Event;

public class EventAnalysisResult
{
	public State state;
	public HashMap<UUID, TransitionAnalysisResult> analysis = new HashMap<UUID, TransitionAnalysisResult>();
	public ArrayList<Event> consumedEvents = new ArrayList<Event>();

	public EventAnalysisResult(State s)
	{
		this.state = s;
	}

	public boolean isPlacePossibleAccordingToTransitions()
	{
		boolean res = false;

		return res;
	}

	public ArrayList<Place> getPossiblePlaces()
	{
		ArrayList<Place> res = new ArrayList<Place>();
		places: for (Place p : state.getRunsOn().getPlaces())
		{
			ArrayList<Event> ce = new ArrayList<Event>();
			if (state.getRepresents().multipleTransitionHandling() == MultipleTransitionsHandlingMode.AND)
			{
				for (TransitionAnalysisResult tra : this.analysis.values())
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

			if (state.getRepresents().multipleTransitionHandling() == MultipleTransitionsHandlingMode.OR)
			{
				for (TransitionAnalysisResult tra : this.analysis.values())
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
}
