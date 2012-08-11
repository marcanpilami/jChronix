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

import java.util.ArrayList;
import java.util.List;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.EventAnalysisResult;
import org.oxymores.chronix.engine.Runner;

public class ActiveNodeBase extends ConfigurableBase {
	private static final long serialVersionUID = 2317281646089939267L;
	private static Logger log = Logger.getLogger(ActiveNodeBase.class);

	protected String description;
	protected String name;

	// ////////////////////////////////////////////////////////////////////////////
	// Stupid get/set
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

	// Helper function (could be overloaded) returning something intelligible
	// designating the element that is run by this source
	public String getCommandName(PipelineJob pj, Runner sender, ChronixContext ctx) {
		return null;
	}

	// stupid get/set
	// ////////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////////////
	// Relationship traversing

	public ArrayList<State> getClientStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (State s : this.application.getStates()) {
			if (s.represents == this)
				res.add(s);
		}
		return res;
	}

	// Relationship traversing
	// ////////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////////////
	// Event analysis

	// Do the given events allow for a transition originating from a state
	// representing this source?
	public EventAnalysisResult createdEventRespectsTransitionOnPlace(Transition tr, List<Event> events, Place p) {
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

	// Do the given events allow the execution of a given State representing
	// this source? (uses createdEventRespectsTransitionOnPlace)
	public EventAnalysisResult isStateExecutionAllowed(State s, Event evt, EntityManager em, MessageProducer pjProducer, Session session,
			ChronixContext ctx) {
		EventAnalysisResult res = new EventAnalysisResult();
		EventAnalysisResult tmp;

		// Get session events
		TypedQuery<Event> q = em.createQuery("SELECT e FROM Event e WHERE e.level0Id = ?1 AND e.level1Id = ?2", Event.class);
		q.setParameter(1, evt.getLevel0IdU().toString());
		q.setParameter(2, evt.getLevel1IdU().toString());
		List<Event> sessionEvents2 = q.getResultList();

		// Remove consumed events (first filter: those which are completely
		// consumed)
		List<Event> sessionEvents = new ArrayList<Event>();
		for (Event e : sessionEvents2) {
			for (Place p : s.runsOn.places) {
				if (!e.wasConsumedOnPlace(p, s) && !sessionEvents.contains(e))
					sessionEvents.add(e);
			}
		}

		// The current event may not yet be DB persisted
		if (!sessionEvents.contains(evt))
			sessionEvents.add(evt);

		// Analysis
		if (s.parallel) {
			// In this case, only check for one place at a time
			log.debug(String.format("State %s (%s - chain %s) is parallel enabled", this.getId(), s.represents.getName(), s.chain.getName()));

			for (Place p : s.runsOn.places) {
				if (p.node.getHost() != s.application.getLocalNode())
					continue;
				log.debug(String.format("Event %s analysis: should // state %s (%s - chain %s) be run on place %s?", evt.getId(), s.getId(),
						s.represents.getName(), s.chain.getName(), p.getName()));

				tmp = new EventAnalysisResult();
				tmp.res = true;
				for (Transition tr : s.trReceivedHere) {
					tmp.add(tr.isTransitionAllowed(sessionEvents, p));
					if (!tmp.res) {
						log.debug(String.format("State %s (%s - chain %s) is NOT allowed to run on place %s", s.getId(), s.represents.getName(),
								s.chain.getName(), p.name));
						continue;
					}
				}

				// Transitions are OK... what about calendars?
				if (!s.canRunAccordingToCalendarOnPlace(em, p))
					continue;

				// If here, everything's OK
				log.debug(String.format("State (%s - chain %s) is triggered by the event on place %s! Analysis has consumed %s events.",
						s.represents.getName(), s.chain.getName(), p.name, tmp.consumedEvents.size()));
				ArrayList<Place> temp = new ArrayList<Place>();
				temp.add(p);
				s.consumeEvents(res.consumedEvents, temp, em);
				s.runFromEngine(p, em, pjProducer, session, evt);
				res.add(tmp);
			}
		} else {
			// In this case, all incoming transitions must be OK for this
			// State to run
			log.debug(String.format("State %s (%s - chain %s) is not parallel enabled. Analysing with %s events", s.getId(), s.represents.getName(),
					s.chain.getName(), sessionEvents.size()));

			ArrayList<Place> places = new ArrayList<Place>();

			// Check transitions
			res.res = true; // we will do logical ANDs
			for (Transition tr : s.trReceivedHere) {
				res.add(tr.isTransitionAllowed(sessionEvents, null));
				// null: no place targeting needed in not // case
				if (!res.res) {
					log.debug(String.format("State %s (%s - chain %s) is NOT allowed to run due to transition from %s", s.getId(),
							s.represents.getName(), s.chain.getName(), tr.stateFrom.represents.name));
					return new EventAnalysisResult(); // not possible
				}
			}

			// Check calendar
			for (Place p : s.runsOn.places) {
				if (s.canRunAccordingToCalendarOnPlace(em, p))
					places.add(p);
			}

			// Go
			if (places.size() > 0) {
				log.debug(String.format(
						"State (%s - chain %s) is triggered by the event on %s of its places! Analysis has consumed %s events on these places.",
						s.represents.getName(), s.chain.getName(), places.size(), res.consumedEvents.size()));

				s.consumeEvents(res.consumedEvents, places, em);
				for (Place p : places) {
					if (p.node.getHost() == s.application.getLocalNode())
						s.runFromEngine(p, em, pjProducer, session, evt);
				}
				return res;
			} else
				return new EventAnalysisResult();
		}
		return res;
	}

	//
	// ////////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////////////
	// Methods called before and after run

	// Run - phase 1
	// Responsible for parameters resolution.
	// Default implementation resolves all parameters. Should usually be called
	// by overloads.
	public void prepareRun(PipelineJob pj, Runner sender, ChronixContext ctx) {
		for (Parameter p : this.parameters) {
			p.resolveValue(ctx, sender, pj);
		}
	}

	// ?
	public void endOfRun(PipelineJob pj, Runner sender, ChronixContext ctx, EntityManager em) {
		log.info("end of run");
	}

	// Called before external run (i.e. sending the PJ to the runner agent)
	// Supposed to do local operations only.
	// Used by active nodes which influence the scheduling itself rather than
	// run a payload.
	// Called within an open JPA transaction.
	public void internalRun(EntityManager em, ChronixContext ctx, PipelineJob pj, Runner runner) {
		return; // Do nothing by default.
	}

	public DateTime selfTrigger(MessageProducer eventProducer, Session jmsSession, ChronixContext ctx, EntityManager em) throws Exception {
		throw new NotImplementedException();
	}

	//
	// ////////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////////////////////
	// Flags (engine and runner)

	// How should the runner agent run this source? (shell command, sql through
	// JDBC, ...)
	public String getActivityMethod() {
		return "None";
	}

	// Should it be run by a runner agent?
	public boolean hasPayload() {
		return false;
	}

	// Should the node execution results be visible in the history table?
	public boolean visibleInHistory() {
		return true;
	}

	// Should it be executed by the self-trigger agent?
	public boolean selfTriggered() {
		return false;
	}
	//
	// ////////////////////////////////////////////////////////////////////////////
}
