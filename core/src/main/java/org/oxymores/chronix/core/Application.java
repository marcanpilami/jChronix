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
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.active.*;
import org.oxymores.chronix.exceptions.ChronixNoLocalNode;

@XmlRootElement
public class Application extends ChronixObject {
	private static final long serialVersionUID = 338399439626386055L;
	private static Logger log = Logger.getLogger(Application.class);

	protected String name, description;

	protected Hashtable<UUID, Place> places;
	protected Hashtable<UUID, PlaceGroup> groups;
	protected Hashtable<UUID, ExecutionNode> nodes;
	protected Hashtable<UUID, ActiveNodeBase> activeElements;
	protected Hashtable<UUID, Parameter> parameters;
	protected Hashtable<UUID, Calendar> calendars;
	protected Hashtable<UUID, ClockRRule> rrules;

	private transient ExecutionNode localNode;
	public transient ExecutionNode consoleNode;

	public Application() {
		super();
		this.places = new Hashtable<UUID, Place>();
		this.groups = new Hashtable<UUID, PlaceGroup>();
		this.nodes = new Hashtable<UUID, ExecutionNode>();
		this.activeElements = new Hashtable<UUID, ActiveNodeBase>();
		this.parameters = new Hashtable<UUID, Parameter>();
		this.calendars = new Hashtable<UUID, Calendar>();
		this.rrules = new Hashtable<UUID, ClockRRule>();

		// Basic elements
		ActiveNodeBase tmp = new And();
		tmp.setName("AND");
		tmp.setDescription("AND logical door - unique for the whole application");
		this.activeElements.put(tmp.getId(), tmp);
		tmp = new Or();
		tmp.setName("OR");
		tmp.setDescription("OR logical door - unique for the whole application");
		this.activeElements.put(tmp.getId(), tmp);
		tmp = new ChainEnd();
		this.activeElements.put(tmp.getId(), tmp);
		tmp = new ChainStart();
		this.activeElements.put(tmp.getId(), tmp);
	}

	public void setLocalNode(String dns, Integer port) throws ChronixNoLocalNode {
		for (ExecutionNode n : this.nodes.values()) {
			if (n.dns.toUpperCase().equals(dns.toUpperCase()) && n.qPort.equals(port)) {
				this.localNode = n;
				log.info(String.format("Application %s now considers that it is running on node %s (%s - %s)", this.name, n.id, dns, port));
				return;
			}
		}
		throw new ChronixNoLocalNode(dns + ":" + port);
	}

	public ExecutionNode getConsoleNode() {
		for (ExecutionNode n : this.nodes.values())
			if (n.isConsole())
				return n;
		return null;
	}

	public void setname(String name) {
		this.name = name;
	}

	public void addRRule(ClockRRule r) {
		if (!this.rrules.contains(r)) {
			this.rrules.put(r.id, r);
			r.setApplication(this);
		}
	}

	public void addCalendar(Calendar c) {
		if (!this.calendars.contains(c)) {
			this.calendars.put(c.id, c);
			c.setApplication(this);
		}
	}

	public void removeACalendar(Calendar c) {
		this.calendars.remove(c.id);
		c.setApplication(null);
	}

	public void addPlace(Place place) {
		if (!this.places.contains(place)) {
			this.places.put(place.id, place);
			place.setApplication(this);
		}
	}

	public void removePlace(Place place) {
		this.places.remove(place.id);
		place.setApplication(null);
	}

	public void addGroup(PlaceGroup o) {
		if (!this.groups.contains(o)) {
			this.groups.put(o.id, o);
			o.setApplication(this);
		}
	}

	public void removeGroup(PlaceGroup o) {
		this.groups.remove(o.id);
		o.setApplication(null);
	}

	public void addNode(ExecutionNode o) {
		if (!this.nodes.contains(o)) {
			this.nodes.put(o.id, o);
			o.setApplication(this);
		}
	}

	public void removeNode(ExecutionNode o) {
		this.nodes.remove(o.id);
		o.setApplication(null);
	}

	public void addActiveElement(ActiveNodeBase o) {
		if (!this.activeElements.contains(o)) {
			this.activeElements.put(o.id, o);
			o.setApplication(this);
		}
	}

	public void removeActiveElement(ConfigurableBase o) {
		this.activeElements.remove(o.id);
		o.setApplication(null);
	}

	public void addParameter(Parameter o) {
		if (!this.parameters.contains(o)) {
			this.parameters.put(o.id, o);
			o.setApplication(this);
		}
	}

	public void removeParameter(Parameter o) {
		this.parameters.remove(o.id);
		o.setApplication(null);
	}

	public String getName() {
		return name;
	}

	public Hashtable<UUID, Place> getPlaces() {
		return places;
	}

	public Hashtable<UUID, PlaceGroup> getGroups() {
		return groups;
	}

	public Hashtable<UUID, ExecutionNode> getNodes() {
		return nodes;
	}

	public Hashtable<UUID, ActiveNodeBase> getActiveElements() {
		return activeElements;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ArrayList<Chain> getChains() {
		ArrayList<Chain> res = new ArrayList<Chain>();
		for (ActiveNodeBase n : this.activeElements.values()) {
			if (n instanceof Chain)
				res.add((Chain) n);
		}
		return res;
	}

	public ArrayList<State> getStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (Chain c : this.getChains()) {
			res.addAll(c.getStates());
		}
		return res;
	}

	public State getState(UUID id) {
		ArrayList<State> tmp = this.getStates();
		State res = null;
		for (State s : tmp) {
			if (s.id.equals(id)) {
				res = s;
				break;
			}
		}
		return res;
	}

	@SuppressWarnings("unchecked")
	public <T> ArrayList<T> getActiveElements(Class<T> tClass) {
		ArrayList<T> res = new ArrayList<T>();
		for (ActiveNodeBase a : this.activeElements.values()) {
			if (a.getClass().isAssignableFrom(tClass))
				res.add((T) a);
		}
		return res;
	}

	public Place getPlace(UUID id) {
		return this.places.get(id);
	}

	public PlaceGroup getPlaceGroup(UUID id) {
		return this.groups.get(id);
	}

	public ExecutionNode getNode(UUID id) {
		return this.nodes.get(id);
	}

	public ActiveNodeBase getActiveNode(UUID id) {
		return this.activeElements.get(id);
	}

	public Calendar getCalendar(UUID id) {
		return this.calendars.get(id);
	}

	public Parameter getParameter(UUID id) {
		return this.parameters.get(id);
	}

	public List<ExecutionNode> getNodesList() {
		return new ArrayList<ExecutionNode>(this.nodes.values());
	}

	public List<Place> getPlacesList() {
		return new ArrayList<Place>(this.places.values());
	}

	public List<PlaceGroup> getGroupsList() {
		return new ArrayList<PlaceGroup>(this.groups.values());
	}

	public ExecutionNode getLocalNode() {
		return localNode;
	}

	public ArrayList<Calendar> getCalendars() {
		return new ArrayList<Calendar>(this.calendars.values());
	}

	@SuppressWarnings("unused")
	private void setLocalNode(ExecutionNode localNode) {
		this.localNode = localNode;
	}
}
