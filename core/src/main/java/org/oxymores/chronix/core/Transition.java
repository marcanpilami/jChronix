package org.oxymores.chronix.core;

import java.util.UUID;

public class Transition extends ActiveNodeBase {

	private static final long serialVersionUID = 1968186705525199010L;

	protected Integer guard1;
	protected String guard2, guard3;
	protected UUID guard4;

	protected State stateFrom, stateTo;

	public Integer getGuard1() {
		return guard1;
	}

	public void setGuard1(Integer guard1) {
		this.guard1 = guard1;
	}

	public String getGuard2() {
		return guard2;
	}

	public void setGuard2(String guard2) {
		this.guard2 = guard2;
	}

	public String getGuard3() {
		return guard3;
	}

	public void setGuard3(String guard3) {
		this.guard3 = guard3;
	}

	public UUID getGuard4() {
		return guard4;
	}

	public void setGuard4(UUID guard4) {
		this.guard4 = guard4;
	}

	public State getStateFrom() {
		return stateFrom;
	}

	public void setStateFrom(State stateFrom) {
		if (this.stateFrom == null || stateFrom != this.stateFrom) {
			this.stateFrom = stateFrom;
			stateFrom.addTransitionFromHere(this);
		}
	}

	public State getStateTo() {
		return stateTo;
	}

	public void setStateTo(State stateTo) {
		if (this.stateTo == null || stateTo != this.stateTo) {
			this.stateTo = stateTo;
			stateTo.addTransitionReceivedHere(this);
		}
	}
}
