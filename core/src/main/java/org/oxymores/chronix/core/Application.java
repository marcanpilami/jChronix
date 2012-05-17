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
import javax.xml.bind.annotation.XmlRootElement;

import org.oxymores.chronix.core.active.*;

@XmlRootElement
public class Application extends ChronixObject {
	private static final long serialVersionUID = 338399439626386055L;

	protected String name, description;

	protected ArrayList<Place> places;
	protected ArrayList<PlaceGroup> groups;
	protected ArrayList<ExecutionNode> nodes;
	protected ArrayList<ActiveNodeBase> activeElements;
	protected ArrayList<Parameter> parameters;

	public Application() {
		super();
		this.places = new ArrayList<Place>();
		this.groups = new ArrayList<PlaceGroup>();
		this.nodes = new ArrayList<ExecutionNode>();
		this.activeElements = new ArrayList<ActiveNodeBase>();
		this.parameters = new ArrayList<Parameter>();

		// Basic elements
		this.activeElements.add(new And());
		this.activeElements.add(new Or());
		this.activeElements.add(new ChainEnd());
		this.activeElements.add(new ChainStart());
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

	public void addActiveElement(ActiveNodeBase o) {
		if (!this.activeElements.contains(o)) {
			this.activeElements.add(o);
			o.setApplication(this);
		}
	}

	public void removeActiveElement(ConfigurableBase o) {
		try {
			this.activeElements.remove(o);
		} finally {
			o.setApplication(null);
		}
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

	public ArrayList<ActiveNodeBase> getActiveElements() {
		return activeElements;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
