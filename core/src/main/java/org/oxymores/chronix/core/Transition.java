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

package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.engine.PlaceAnalysisResult;
import org.oxymores.chronix.engine.TransitionAnalysisResult;

public class Transition extends ApplicationObject
{
	private static final long serialVersionUID = 1968186705525199010L;

	protected Integer guard1;
	protected String guard2, guard3;
	protected UUID guard4;
	protected boolean calendarAware = false;

	protected State stateFrom, stateTo;
	protected Chain chain;

	public Integer getGuard1()
	{
		return guard1;
	}

	public void setGuard1(Integer guard1)
	{
		this.guard1 = guard1;
	}

	public String getGuard2()
	{
		return guard2;
	}

	public void setGuard2(String guard2)
	{
		this.guard2 = guard2;
	}

	public String getGuard3()
	{
		return guard3;
	}

	public void setGuard3(String guard3)
	{
		this.guard3 = guard3;
	}

	public UUID getGuard4()
	{
		return guard4;
	}

	public void setGuard4(UUID guard4)
	{
		this.guard4 = guard4;
	}

	public boolean isCalendarAware()
	{
		return calendarAware;
	}

	public void setCalendarAware(boolean calendarAware)
	{
		this.calendarAware = calendarAware;
	}

	public State getStateFrom()
	{
		return stateFrom;
	}

	public void setStateFrom(State stateFrom)
	{
		if (this.stateFrom == null || stateFrom != this.stateFrom)
		{
			this.stateFrom = stateFrom;
			stateFrom.addTransitionFromHere(this);
		}
	}

	public State getStateTo()
	{
		return stateTo;
	}

	public void setStateTo(State stateTo)
	{
		if (this.stateTo == null || stateTo != this.stateTo)
		{
			this.stateTo = stateTo;
			stateTo.addTransitionReceivedHere(this);
		}
	}

	public Chain getChain()
	{
		return chain;
	}

	public void setChain(Chain chain)
	{
		this.chain = chain;
		if (chain != null)
			chain.addTransition(this);
	}

	public Boolean isTransitionParallelEnabled()
	{
		if (!this.stateFrom.parallel || !this.stateTo.parallel)
			return false;

		if (!this.stateFrom.runsOn.equals(this.stateTo.runsOn))
			return false;

		return true;
	}

	public TransitionAnalysisResult isTransitionAllowed(List<Event> events, EntityManager em)
	{
		TransitionAnalysisResult res = new TransitionAnalysisResult(this);

		for (Place p : this.stateFrom.runsOn.places)
		{
			ArrayList<Event> virginEvents = new ArrayList<Event>();
			for (Event e : events)
			{
				if (!e.wasConsumedOnPlace(p, this.stateTo) && !virginEvents.contains(e))
					virginEvents.add(e);
			}
			res.analysis.put(p.id, this.stateFrom.represents.createdEventRespectsTransitionOnPlace(this, virginEvents, p, em));

			if (!this.isTransitionParallelEnabled() && !res.allowedOnAllPlaces())
			{
				res = new TransitionAnalysisResult(this); // We already know the transition is KO, so no need to continue
				for (Place pp : this.stateFrom.runsOn.places)
				{
					res.analysis.put(pp.id, new PlaceAnalysisResult(pp)); // Create a FORBIDDEN result on all places
				}
				return res;
			}
		}
		return res;
	}
}
