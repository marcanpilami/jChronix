/**
 * @author Marc-Antoine Gouillart
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

package org.oxymores.chronix.core;

import java.util.List;

import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.engine.EventAnalysisResult;

public class ActiveNodeBase extends ConfigurableBase {
	private static final long serialVersionUID = 2317281646089939267L;

	protected String description;
	protected String name;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EventAnalysisResult createdEventRespectsTransitionOnPlace(
			Transition tr, List<Event> events, Place p) {
		EventAnalysisResult res = new EventAnalysisResult();
		res.res = false;

		for (Event e : events) {
			if (!e.getActiveID().equals(id.toString())) {
				// Only accept events from this source
				continue;
			}

			if (!e.getPlaceID().equals(p.id.toString())) {
				// Only accept events on the analyzed place
				continue;
			}

			// Check guards
			if (tr.guard1 != null && !tr.guard1.equals(e.getConditionData1())) {
				continue;
			}
			if (tr.guard2 != null && !tr.guard2.equals(e.getConditionData2())) {
				continue;
			}
			if (tr.guard3 != null && !tr.guard3.equals(e.getConditionData3())) {
				continue;
			}
			if (tr.guard4 != null && !tr.guard4.equals(e.getConditionData4U())) {
				continue;
			}

			// If here: the event is OK for the given transition on the given
			// place.
			res.consumedEvents.add(e);
			res.res = true;
			return res;
		}

		// If here: no event allows the transition on the given place
		return res;
	}
}
