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
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.EventAnalysisResult;
import org.oxymores.chronix.engine.RunDescription;
import org.oxymores.chronix.engine.Runner;

public class ActiveNodeBase extends ConfigurableBase {
	private static final long serialVersionUID = 2317281646089939267L;
	private static Logger log = Logger.getLogger(State.class);

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

	public EventAnalysisResult isStateExecutionAllowed(State s, Event evt,
			EntityManager em, MessageProducer pjProducer, Session session,
			ChronixContext ctx) {
		EventAnalysisResult res = new EventAnalysisResult();
		EventAnalysisResult tmp;

		// Get session events
		Query q = em.createQuery("SELECT e FROM Event e WHERE e.level0Id = ?1");
		q.setParameter(1, evt.getLevel0IdU().toString());
		@SuppressWarnings("unchecked")
		List<Event> sessionEvents2 = q.getResultList();
		List<Event> sessionEvents = new ArrayList<Event>();
		
		/*if (s.usesCalendar())
		{
			CalendarDay currentStateDay = 
			for (Event event : sessionEvents2) {
				if (event.getCalendarOccurrenceID().equals())
			}
		}
		else*/
			sessionEvents.addAll(sessionEvents2); // OpenJPA lists are read only!
		sessionEvents.add(evt); // The current event is not yet db persisted

		if (s.parallel) {
			// In this case, only
			log.debug(String.format(
					"State %s (%s - chain %s) is parallel enabled",
					this.getId(), s.represents.getName(), s.chain.getName()));

			for (Place p : s.runsOn.places) {
				log.debug(String
						.format("Event %s analysis: should // state %s (%s - chain %s) be run on place %s?",
								evt.getId(), s.getId(), s.represents.getName(),
								s.chain.getName(), p.getName()));

				tmp = new EventAnalysisResult();
				tmp.res = true;
				for (Transition tr : s.trReceivedHere) {
					tmp.add(tr.isTransitionAllowed(sessionEvents, p));
					if (!tmp.res) {
						log.debug(String
								.format("State %s (%s - chain %s) is NOT allowed to run on place %s",
										s.getId(), s.represents.getName(),
										s.chain.getName(), p.name));
						continue;
					}
				}
				log.debug(String
						.format("State (%s - chain %s) is triggered by the event on place %s! Analysis has consumed %s events.",
								s.represents.getName(), s.chain.getName(),
								p.name, tmp.consumedEvents.size()));
				ArrayList<Place> temp = new ArrayList<Place>();
				temp.add(p);
				s.consumeEvents(res.consumedEvents, temp, em);
				s.run(p, em, pjProducer, session, evt);
				res.add(tmp);
			}
		} else {
			// In this case, all incoming transitions must be OK for this
			// State to run
			log.debug(String.format(
					"State %s (%s - chain %s) is not parallel enabled",
					s.getId(), s.represents.getName(), s.chain.getName()));
			res.res = true; // we will do logical ANDs
			for (Transition tr : s.trReceivedHere) {
				res.add(tr.isTransitionAllowed(sessionEvents, null));
				// null: no place targeting needed in not // case
				if (!res.res) {
					log.debug(String.format(
							"State %s (%s - chain %s) is NOT allowed to run",
							s.getId(), s.represents.getName(),
							s.chain.getName()));
					return new EventAnalysisResult(); // not possible
				}
			}

			log.debug(String
					.format("State (%s - chain %s) is triggered by the event on all (%s) its places! Analysis has consumed %s events.",
							s.represents.getName(), s.chain.getName(),
							s.runsOn.places.size(), res.consumedEvents.size()));

			s.consumeEvents(res.consumedEvents, s.runsOn.places, em);
			for (Place p : s.runsOn.places) {
				s.run(p, em, pjProducer, session, evt);
			}
			return res;
		}
		return res;
	}

	// Run - phase 1
	// Responsible for parameters resolution.
	// Default implementation resolves all parameters. Should usually be called
	// by overloads.
	public void prepareRun(PipelineJob pj, Runner sender, ChronixContext ctx) {
		for (Parameter p : this.parameters) {
			p.resolveValue(ctx, sender, pj);
		}
	}

	public String getCommandName(PipelineJob pj, Runner sender,
			ChronixContext ctx) {
		return null;
	}

	// Run - phase 2
	// Called once all parameters are resolved and stored in the PJ.
	// From the PJ, it is supposed to create the final RD which will be given to
	// the runner. Should usually be overloaded - default does nothing.
	public RunDescription finalizeRunDescription(PipelineJob pj, Runner sender,
			ChronixContext ctx) {
		return null;
	}

	// Useless
	public void run(PipelineJob pj, Runner sender, ChronixContext ctx,
			EntityManager em) {
		log.info("running"); // should "usually" be overloaded
	}

	// ?
	public void endOfRun(PipelineJob pj, Runner sender, ChronixContext ctx,
			EntityManager em) {
		log.info("end of run");
	}

	public String getActivityMethod() {
		return "None";
	}

	public boolean hasPayload() {
		return false;
	}
}
