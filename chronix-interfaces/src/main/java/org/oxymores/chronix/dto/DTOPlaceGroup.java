package org.oxymores.chronix.dto;

import java.util.ArrayList;

public class DTOPlaceGroup {
	public String id, name, description;
	public ArrayList<String> places;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

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

	public ArrayList<String> getPlaces() {
		return places;
	}

	public void setPlaces(ArrayList<String> places) {
		this.places = places;
	}

}
