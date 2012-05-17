package org.oxymores.chronix.core;

import java.util.ArrayList;

public class AutoSequence extends ApplicationObject {

	private static final long serialVersionUID = -4652472532871950327L;

	protected String name, description;
	protected ArrayList<State> usedInStates;

	public AutoSequence() {
		super();
		usedInStates = new ArrayList<State>();
	}

	// Only called from State.addSequence
	void s_addStateUsing(State s) {
		usedInStates.add(s);
	}

	// Only called from State.addSequence
	void s_removeStateUsing(State s) {
		try {
			usedInStates.remove(s);
		} finally { // do nothing if asked to remove a non existent state
		}
	}

}
