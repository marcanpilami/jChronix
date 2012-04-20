package org.oxymores.chronix.dto;

import java.math.BigInteger;
import java.util.ArrayList;

public class DTOChain {
	public String name;
	public String description;
	public BigInteger id;
	
	public ArrayList<DTOState> states;

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

	public BigInteger getId() {
		return id;
	}

	public void setId(BigInteger id) {
		this.id = id;
	}

	public ArrayList<DTOState> getStates() {
		return states;
	}

	public void setStates(ArrayList<DTOState> states) {
		this.states = states;
	}
}
