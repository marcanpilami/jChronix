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

public class State extends ConfigurableBase {

	private static final long serialVersionUID = -2640644872229489081L;

	protected Boolean parallel;

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
		// Note: there can be multiple transitions between two states.
		Transition t = new Transition();
		t.setStateFrom(this);
		t.setStateTo(target);
		t.setGuard1(0);
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
}
