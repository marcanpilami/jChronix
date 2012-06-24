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
import java.util.ArrayList;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.engine.EventAnalysisResult;

public class State extends ConfigurableBase {
	private static Logger log = Logger.getLogger(State.class);

	private static final long serialVersionUID = -2640644872229489081L;

	protected Boolean parallel = false;

	// GUI data
	protected Integer x, y;

	// Time limits
	protected Integer warnAfterMn, killAfterMn, maxPipeWaitTime,
			eventValidityMn;

	// The active element represented by this State
	protected ActiveNodeBase represents;

	// The chain it belongs to
	protected Chain chain;

	// Transitions
	protected ArrayList<Transition> trFromHere, trReceivedHere;

	// Exclusive states
	protected ArrayList<State> exclusiveStates;

	// Runs on a group. Groups are defined in a separate graph, so get it by ID
	// and not by reference.
	protected PlaceGroup runsOn;

	// Sequences
	protected ArrayList<AutoSequence> sequences;
	protected ArrayList<Calendar> calendars;

	public State() {
		super();
		this.exclusiveStates = new ArrayList<State>();
		this.trFromHere = new ArrayList<Transition>();
		this.trReceivedHere = new ArrayList<Transition>();
		this.sequences = new ArrayList<AutoSequence>();
		this.calendars = new ArrayList<Calendar>();
	}

	public PlaceGroup getRunsOn() {
		return this.runsOn;
	}

	public ArrayList<Place> getRunsOnPlaces() {
		return this.runsOn.getPlaces();
	}

	public ArrayList<ExecutionNode> getRunsOnExecutionNodes() {
		ArrayList<ExecutionNode> res = new ArrayList<ExecutionNode>();
		for (Place p : this.runsOn.getPlaces()) {
			if (!res.contains(p.getNode())) {
				res.add(p.getNode());
			}
		}
		return res;
	}

	public ArrayList<ExecutionNode> getRunsOnPhysicalNodes() {
		ArrayList<ExecutionNode> all = getRunsOnExecutionNodes();
		ArrayList<ExecutionNode> res = getRunsOnExecutionNodes();
		for (ExecutionNode n : all) {
			if (n.isHosted()) {
				if (!res.contains(n.getHost()))
					res.add(n.getHost());
			} else {
				// Not hosted - true Physical Node
				if (!res.contains(n))
					res.add(n);
			}
		}
		return res;
	}

	public void setRunsOn(PlaceGroup group) {
		this.runsOn = group;
	}

	public void addTransitionFromHere(Transition tr) {
		if (!this.trFromHere.contains(tr)) {
			this.trFromHere.add(tr);
			tr.setStateFrom(this);
		}
	}

	public void addTransitionReceivedHere(Transition tr) {
		if (!this.trReceivedHere.contains(tr)) {
			this.trReceivedHere.add(tr);
			tr.setStateTo(this);
		}
	}

	public Transition connectTo(State target) {
		return connectTo(target, 0, null, null, null);
	}

	public Transition connectTo(State target, Integer guard1) {
		return connectTo(target, guard1, null, null, null);
	}

	public Transition connectTo(State target, Integer guard1, String guard2,
			String guard3, UUID guard4) {
		// Note: there can be multiple transitions between two states.
		Transition t = new Transition();
		t.setStateFrom(this);
		t.setStateTo(target);
		t.setGuard1(guard1);
		t.setGuard2(guard2);
		t.setGuard3(guard3);
		t.setGuard4(guard4);
		t.setApplication(this.application);
		this.chain.addTransition(t);
		return t;
	}

	public ArrayList<Transition> getTrFromHere() {
		return trFromHere;
	}

	public ArrayList<State> getClientStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition t : trFromHere) {
			res.add(t.stateTo);
		}
		return res;
	}

	public ArrayList<State> getParentStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition t : trReceivedHere) {
			res.add(t.stateFrom);
		}
		return res;
	}

	public ArrayList<Transition> getTrReceivedHere() {
		return trReceivedHere;
	}

	public ArrayList<AutoSequence> getSequences() {
		return sequences;
	}

	public ArrayList<Calendar> getCalendars() {
		return calendars;
	}

	public Boolean getParallel() {
		return parallel;
	}

	public void setParallel(Boolean parallel) {
		this.parallel = parallel;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getWarnAfterMn() {
		return warnAfterMn;
	}

	public void setWarnAfterMn(Integer warnAfterMn) {
		this.warnAfterMn = warnAfterMn;
	}

	public Integer getKillAfterMn() {
		return killAfterMn;
	}

	public void setKillAfterMn(Integer killAfterMn) {
		this.killAfterMn = killAfterMn;
	}

	public Integer getMaxPipeWaitTime() {
		return maxPipeWaitTime;
	}

	public void setMaxPipeWaitTime(Integer maxPipeWaitTime) {
		this.maxPipeWaitTime = maxPipeWaitTime;
	}

	public Integer getEventValidityMn() {
		return eventValidityMn;
	}

	public void setEventValidityMn(Integer eventValidityMn) {
		this.eventValidityMn = eventValidityMn;
	}

	public ActiveNodeBase getRepresents() {
		return represents;
	}

	public void setRepresents(ActiveNodeBase represents) {
		this.represents = represents;
	}

	public Chain getChain() {
		return chain;
	}

	public void setChain(Chain chain) {
		this.chain = chain;
		this.application = chain.getApplication();
		if (chain != null)
			chain.addState(this);
	}

	public ArrayList<State> getExclusiveStates() {
		return exclusiveStates;
	}

	public void addSequence(AutoSequence s) {
		s.s_addStateUsing(this);
		this.sequences.add(s);
	}

	public void removeSequence(AutoSequence s) {
		try {
			this.sequences.remove(s);
		} finally {
			s.s_removeStateUsing(this);
		}
	}

	public void addCalendar(Calendar c) {
		c.s_addStateUsing(this);
		this.calendars.add(c);
	}

	public void removeCalendar(Calendar c) {
		try {
			this.calendars.remove(c);
		} finally {
			c.s_removeStateUsing(this);
		}
	}

	void isOutgoingTransitionAllowed(Transition tr, List<Event> events,
			Place targetPlace) {

	}

	private boolean isParallelCompatible() {
		if (!this.parallel)
			return false;

		return false;
	}

	public void isStateExecutionAllowed(Event evt, EntityManager em,
			ChronixContext ctx, State stateFrom, ActiveNodeBase activeFrom,
			Application appliFrom) {
		EventAnalysisResult res = new EventAnalysisResult();

		// Get session events
		Query q = em.createQuery("SELECT e FROM Event e WHERE e.level0Id = ?1");
		q.setParameter(1, evt.getLevel0IdU().toString());
		List<Event> sessionEvents2 = q.getResultList();
		List<Event> sessionEvents = new ArrayList<Event>();
		sessionEvents.addAll(sessionEvents2); // OpenJPA lists are read only!
		sessionEvents.add(evt); // The current event is not yet db persisted

		if (this.parallel) {
			// In this case, only
			log.debug(String.format(
					"State %s (%s - chain %s) is parallel enabled",
					this.getId(), this.represents.getName(),
					this.chain.getName()));

			for (Place p : this.runsOn.places) {
				log.debug(String
						.format("Event %s analysis: should // state %s (%s - chain %s) be run on place %s?",
								evt.getId(), this.getId(),
								this.represents.getName(),
								this.chain.getName(), p.getName()));

				res = new EventAnalysisResult();
				res.res = true;
				for (Transition tr : this.trReceivedHere) {
					res.add(tr.isTransitionAllowed(sessionEvents, p));
					if (!res.res) {
						log.debug(String
								.format("State %s (%s - chain %s) is NOT allowed to run on place %s",
										this.getId(),
										this.represents.getName(),
										this.chain.getName(), p.name));
						continue;
					}
				}
				log.debug(String
						.format("State (%s - chain %s) is triggered by the event on place %s! Analysis has consumed %s events.",
								this.represents.getName(),
								this.chain.getName(), p.name,
								res.consumedEvents.size()));
			}
		} else {
			// In this case, all incoming transitions must be OK for this
			// State to run
			log.debug(String.format(
					"State %s (%s - chain %s) is not parallel enabled",
					this.getId(), this.represents.getName(),
					this.chain.getName()));
			res.res = true; // we will do logical ANDs
			for (Transition tr : this.trReceivedHere) {
				res.add(tr.isTransitionAllowed(sessionEvents, null));
				// null: no place targeting needed in not // case
				if (!res.res) {
					log.debug(String.format(
							"State %s (%s - chain %s) is NOT allowed to run",
							this.getId(), this.represents.getName(),
							this.chain.getName()));
					return; // not possible
				}
			}

			log.debug(String
					.format("State (%s - chain %s) is triggered by the event on all (%s) its places! Analysis has consumed %s events.",
							this.represents.getName(), this.chain.getName(),
							this.runsOn.places.size(),
							res.consumedEvents.size()));

		}
		// log.debug(String.format("", ));
	}
}
