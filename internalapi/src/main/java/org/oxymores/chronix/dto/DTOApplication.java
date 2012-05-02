package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.UUID;

public class DTOApplication {

	public String name;
	public String id;

	public ArrayList<DTOPlace> places;
	public ArrayList<DTOPlaceGroup> groups;
	public ArrayList<DTOParameter> parameters;
	public ArrayList<DTOChain> chains;
	public ArrayList<DTOShellCommand> shells;

	protected UUID marsu;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public ArrayList<DTOPlace> getPlaces() {
		return places;
	}

	public void setPlaces(ArrayList<DTOPlace> places) {
		this.places = places;
	}

	public ArrayList<DTOPlaceGroup> getGroups() {
		return groups;
	}

	public void setGroups(ArrayList<DTOPlaceGroup> groups) {
		this.groups = groups;
	}

	public ArrayList<DTOParameter> getParameters() {
		return parameters;
	}

	public void setParameters(ArrayList<DTOParameter> parameters) {
		this.parameters = parameters;
	}

	public ArrayList<DTOChain> getChains() {
		return chains;
	}

	public void setChains(ArrayList<DTOChain> chains) {
		this.chains = chains;
	}

	public ArrayList<DTOShellCommand> getShells() {
		return shells;
	}

	public void setShells(ArrayList<DTOShellCommand> shells) {
		this.shells = shells;
	}

	public UUID getMarsu() {
		return marsu;
	}

	public void setMarsu(UUID marsu) {
		this.marsu = marsu;
	}
}
