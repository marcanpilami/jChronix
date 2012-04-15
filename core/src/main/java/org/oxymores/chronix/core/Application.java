package org.oxymores.chronix.core;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlRootElement;

import org.oxymores.chronix.core.active.*;

@XmlRootElement
public class Application extends MetaObject {
	private static final long serialVersionUID = 338399439626386055L;

	protected String name;

	protected ArrayList<Place> places;
	protected ArrayList<PlaceGroup> groups;
	protected ArrayList<ExecutionNode> nodes;
	protected ArrayList<ConfigNodeBase> elements;
	protected ArrayList<Parameter> parameters;

	public Application() {
		super();
		this.places = new ArrayList<Place>();
		this.groups = new ArrayList<PlaceGroup>();
		this.nodes = new ArrayList<ExecutionNode>();
		this.elements = new ArrayList<ConfigNodeBase>();
		this.parameters = new ArrayList<Parameter>();
		
		// Basic elements
		this.elements.add(new And());
		this.elements.add(new Or());
		this.elements.add(new ChainEnd());
		this.elements.add(new ChainStart());
	}

	public String getname() {
		return name;
	}

	public void setname(String name) {
		this.name = name;
	}

	public void addPlace(Place place) {
		if (!this.places.contains(place)) {
			this.places.add(place);
			place.setApplication(this);
		}
	}

	public void removePlace(Place place) {
		this.places.remove(place);
		place.setApplication(null);
	}

	public void addGroup(PlaceGroup o) {
		if (!this.groups.contains(o)) {
			this.groups.add(o);
			o.setApplication(this);
		}
	}

	public void removeGroup(PlaceGroup o) {
		this.groups.remove(o);
		o.setApplication(null);
	}

	public void addNode(ExecutionNode o) {
		if (!this.nodes.contains(o)) {
			this.nodes.add(o);
			o.setApplication(this);
		}
	}

	public void removeNode(ExecutionNode o) {
		this.nodes.remove(o);
		o.setApplication(null);
	}

	public void addElement(ConfigNodeBase o) {
		if (!this.elements.contains(o)) {
			this.elements.add(o);
			o.setApplication(this);
		}
	}

	public void removeElement(ConfigNodeBase o) {
		this.elements.remove(o);
		o.setApplication(null);
	}
	
	public void addParameter(Parameter o) {
		if (!this.parameters.contains(o)) {
			this.parameters.add(o);
			o.setApplication(this);
		}
	}

	public void removeParameter(Parameter o) {
		this.parameters.remove(o);
		o.setApplication(null);
	}

	public String getName() {
		return name;
	}

	public ArrayList<Place> getPlaces() {
		return places;
	}

	public ArrayList<PlaceGroup> getGroups() {
		return groups;
	}

	public ArrayList<ExecutionNode> getNodes() {
		return nodes;
	}

	public ArrayList<ConfigNodeBase> getElements() {
		return elements;
	}
}
