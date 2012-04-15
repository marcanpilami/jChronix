package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.UUID;

import org.oxymores.chronix.exceptions.ChronixInconsistentMetadataException;

public class State extends ConfigNodeBase {
	private static final long serialVersionUID = -2640644872229489081L;

	protected Boolean parallel;
	
	// GUI data
	protected Integer X, Y;
	
	// Time limits
	protected Integer WarnAfterMn, KillAfterMn, MaxPipeWaitTime, EventValidityMn;
	
	// The active element represented by this State
	protected ActiveNodeBase represents;
	
	// The chain it belongs to
	protected Chain chain;
	
	// Transitions
	protected ArrayList<Transition> trFromHere, trReceivedHere;
	
	// Exclusive states
	protected ArrayList<State> exclusiveStates;
	
	// Runs on a group. Groups are defined in a separate graph, so get it by ID and not by reference. 
	protected UUID placeGroupId;
	
	public State()
	{
		super();
		this.exclusiveStates = new ArrayList<State>();
		this.trFromHere = new ArrayList<Transition>();
		this.trReceivedHere = new ArrayList<Transition>();
	}
	
	public PlaceGroup getRunsOn() throws ChronixInconsistentMetadataException
	{
		for (PlaceGroup group : this.Application.getGroups())
		{
			if (group.getId().equals(this.placeGroupId))
				return group;
		}
		throw new ChronixInconsistentMetadataException("A State references an inexistent PlaceGroup");
	}
	
	public void setRunsOn(PlaceGroup group)
	{
		this.placeGroupId = group.getId();
	}
	
	public void addTransitionFromHere(Transition tr)
	{
		if (! this.trFromHere.contains(tr))
		{
			this.trFromHere.add(tr);
			tr.setStateFrom(this);
		}
	}
	
	public void addTransitionReceivedHere(Transition tr)
	{
		if (! this.trReceivedHere.contains(tr))
		{
			this.trReceivedHere.add(tr);
			tr.setStateTo(this);
		}
	}
	
	public Transition connectTo(State target)
	{
		// Note: there can be multiple transitions between two states.
		Transition t = new Transition();
		t.setStateFrom(this);
		t.setStateTo(target);
		t.setGuard1(0);
		return t;
	}
	
	

	public Boolean getParallel() {
		return parallel;
	}

	public void setParallel(Boolean parallel) {
		this.parallel = parallel;
	}

	public Integer getX() {
		return X;
	}

	public void setX(Integer x) {
		X = x;
	}

	public Integer getY() {
		return Y;
	}

	public void setY(Integer y) {
		Y = y;
	}

	public Integer getWarnAfterMn() {
		return WarnAfterMn;
	}

	public void setWarnAfterMn(Integer warnAfterMn) {
		WarnAfterMn = warnAfterMn;
	}

	public Integer getKillAfterMn() {
		return KillAfterMn;
	}

	public void setKillAfterMn(Integer killAfterMn) {
		KillAfterMn = killAfterMn;
	}

	public Integer getMaxPipeWaitTime() {
		return MaxPipeWaitTime;
	}

	public void setMaxPipeWaitTime(Integer maxPipeWaitTime) {
		MaxPipeWaitTime = maxPipeWaitTime;
	}

	public Integer getEventValidityMn() {
		return EventValidityMn;
	}

	public void setEventValidityMn(Integer eventValidityMn) {
		EventValidityMn = eventValidityMn;
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
}
