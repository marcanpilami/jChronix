package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.List;

public class Token extends ApplicationObject {
	private static final long serialVersionUID = 6422487791877618666L;

	String name;
	int count = 1;
	boolean byPlace = false;

	protected ArrayList<State> usedInStates;

	// Constructor
	public Token() {
		super();
		usedInStates = new ArrayList<State>();
	}

	// /////////////////////////////////////////////////
	// Stupid GET/SET
	public boolean isByPlace() {
		return byPlace;
	}

	public void setByPlace(boolean byPlace) {
		this.byPlace = byPlace;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	//
	// /////////////////////////////////////////////////

	// /////////////////////////////////////////////////
	// Relationships
	public List<State> getUsedInStates() {
		return this.usedInStates;
	}

	// Only called from State.addToken
	void s_addStateUsing(State s) {
		usedInStates.add(s);
	}

	// Only called from State.addToken
	void s_removeStateUsing(State s) {
		try {
			usedInStates.remove(s);
		} finally { // do nothing if asked to remove a non existent state
		}
	}
	// relationships
	// /////////////////////////////////////////////////
}
