package org.oxymores.chronix.dto;

import java.util.ArrayList;

public class DTOChain {
	public String name;
	public String description;
	public String id;

	public ArrayList<DTOState> states;
	public ArrayList<DTOTransition> transitions;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ArrayList<DTOState> getStates() {
		return states;
	}

	public void setStates(ArrayList<DTOState> states) {
		this.states = states;
	}

	public ArrayList<DTOTransition> getTransitions() {
		return transitions;
	}

	public void setTransitions(ArrayList<DTOTransition> transitions) {
		this.transitions = transitions;
	}
}
