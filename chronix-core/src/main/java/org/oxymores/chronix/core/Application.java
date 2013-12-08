/**
 * By Marc-Antoine Gouillart, 2012
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.active.And;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.Or;
import org.oxymores.chronix.exceptions.ChronixNoLocalNode;

@XmlRootElement
public class Application extends ChronixObject
{
    private static final long serialVersionUID = 338399439626386055L;
    private static Logger log = Logger.getLogger(Application.class);

    protected String name, description;

    protected Map<UUID, Place> places;
    protected Map<UUID, PlaceGroup> groups;
    protected Map<UUID, ExecutionNode> nodes;
    protected Map<UUID, ActiveNodeBase> activeElements;
    protected Map<UUID, Parameter> parameters;
    protected Map<UUID, Calendar> calendars;
    protected Map<UUID, Token> tokens;
    protected Map<UUID, ClockRRule> rrules;

    protected transient ExecutionNode localNode;
    protected transient ExecutionNode consoleNode;

    public Application()
    {
        super();
        this.places = new HashMap<UUID, Place>();
        this.groups = new HashMap<UUID, PlaceGroup>();
        this.nodes = new HashMap<UUID, ExecutionNode>();
        this.activeElements = new HashMap<UUID, ActiveNodeBase>();
        this.parameters = new HashMap<UUID, Parameter>();
        this.calendars = new HashMap<UUID, Calendar>();
        this.rrules = new HashMap<UUID, ClockRRule>();
        this.tokens = new HashMap<UUID, Token>();

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

    public void setLocalNode(String dns, Integer port) throws ChronixNoLocalNode
    {
        for (ExecutionNode n : this.nodes.values())
        {
            if (n.dns.equalsIgnoreCase(dns) && n.qPort.equals(port))
            {
                this.localNode = n;
                log.info(String.format("Application %s now considers that it is running on node %s (%s - %s)", this.name, n.id, dns, port));
                return;
            }
        }
        throw new ChronixNoLocalNode(dns + ":" + port);
    }

    public ExecutionNode getConsoleNode()
    {
        for (ExecutionNode n : this.nodes.values())
        {
            if (n.isConsole())
            {
                return n;
            }
        }
        return null;
    }

    public void setname(String name)
    {
        this.name = name;
    }

    public void addRRule(ClockRRule r)
    {
        if (!this.rrules.containsValue(r))
        {
            this.rrules.put(r.id, r);
            r.setApplication(this);
        }
    }

    public void addCalendar(Calendar c)
    {
        if (!this.calendars.containsValue(c))
        {
            this.calendars.put(c.id, c);
            c.setApplication(this);
        }
    }

    public void removeACalendar(Calendar c)
    {
        this.calendars.remove(c.id);
        c.setApplication(null);
    }

    public void addToken(Token t)
    {
        if (!this.tokens.containsValue(t))
        {
            this.tokens.put(t.id, t);
            t.setApplication(this);
        }
    }

    public void removeToken(Token t)
    {
        this.tokens.remove(t.id);
        t.setApplication(null);
    }

    public void addPlace(Place place)
    {
        if (!this.places.containsValue(place))
        {
            this.places.put(place.id, place);
            place.setApplication(this);
        }
    }

    public void removePlace(Place place)
    {
        this.places.remove(place.id);
        place.setApplication(null);
    }

    public void addGroup(PlaceGroup o)
    {
        if (!this.groups.containsValue(o))
        {
            this.groups.put(o.id, o);
            o.setApplication(this);
        }
    }

    public void removeGroup(PlaceGroup o)
    {
        this.groups.remove(o.id);
        o.setApplication(null);
    }

    public void addNode(ExecutionNode o)
    {
        if (!this.nodes.containsValue(o))
        {
            this.nodes.put(o.id, o);
            o.setApplication(this);
        }
    }

    public void removeNode(ExecutionNode o)
    {
        this.nodes.remove(o.id);
        o.setApplication(null);
    }

    public void addActiveElement(ActiveNodeBase o)
    {
        if (!this.activeElements.containsValue(o))
        {
            this.activeElements.put(o.id, o);
            o.setApplication(this);
        }
    }

    public void removeActiveElement(ConfigurableBase o)
    {
        this.activeElements.remove(o.id);
        o.setApplication(null);
    }

    public void addParameter(Parameter o)
    {
        if (!this.parameters.containsValue(o))
        {
            this.parameters.put(o.id, o);
            o.setApplication(this);
        }
    }

    public void removeParameter(Parameter o)
    {
        this.parameters.remove(o.id);
        o.setApplication(null);
    }

    public String getName()
    {
        return name;
    }

    public Map<UUID, Place> getPlaces()
    {
        HashMap<UUID, Place> res = new HashMap<UUID, Place>();
        res.putAll(this.places);
        return res;
    }

    public PlaceGroup getGroup(UUID id)
    {
        return this.groups.get(id);
    }

    public Map<UUID, PlaceGroup> getGroups()
    {
        HashMap<UUID, PlaceGroup> res = new HashMap<UUID, PlaceGroup>();
        res.putAll(this.groups);
        return res;
    }

    public Map<UUID, ExecutionNode> getNodes()
    {
        HashMap<UUID, ExecutionNode> res = new HashMap<UUID, ExecutionNode>();
        res.putAll(this.nodes);
        return res;
    }

    public Map<UUID, ActiveNodeBase> getActiveElements()
    {
        HashMap<UUID, ActiveNodeBase> res = new HashMap<UUID, ActiveNodeBase>();
        res.putAll(this.activeElements);
        return res;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<Chain> getChains()
    {
        ArrayList<Chain> res = new ArrayList<Chain>();
        for (ActiveNodeBase n : this.activeElements.values())
        {
            if (n instanceof Chain)
            {
                res.add((Chain) n);
            }
        }
        return res;
    }

    public List<State> getStates()
    {
        ArrayList<State> res = new ArrayList<State>();
        for (Chain c : this.getChains())
        {
            res.addAll(c.getStates());
        }
        return res;
    }

    public State getState(UUID id)
    {
        for (State s : this.getStates())
        {
            if (s.id.equals(id))
            {
                return s;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getActiveElements(Class<T> tClass)
    {
        ArrayList<T> res = new ArrayList<T>();
        for (ActiveNodeBase a : this.activeElements.values())
        {
            if (a.getClass().isAssignableFrom(tClass))
            {
                res.add((T) a);
            }
        }
        return res;
    }

    public Place getPlace(UUID id)
    {
        return this.places.get(id);
    }

    public PlaceGroup getPlaceGroup(UUID id)
    {
        return this.groups.get(id);
    }

    public ExecutionNode getNode(UUID id)
    {
        return this.nodes.get(id);
    }

    public Token getToken(UUID id)
    {
        return this.tokens.get(id);
    }

    public ActiveNodeBase getActiveNode(UUID id)
    {
        return this.activeElements.get(id);
    }

    public Calendar getCalendar(UUID id)
    {
        return this.calendars.get(id);
    }

    public Parameter getParameter(UUID id)
    {
        return this.parameters.get(id);
    }

    public List<ExecutionNode> getNodesList()
    {
        return new ArrayList<ExecutionNode>(this.nodes.values());
    }

    public List<Place> getPlacesList()
    {
        return new ArrayList<Place>(this.places.values());
    }

    public ClockRRule getRRule(UUID id)
    {
        return this.rrules.get(id);
    }

    public List<ClockRRule> getRRulesList()
    {
        return new ArrayList<ClockRRule>(this.rrules.values());
    }

    public List<PlaceGroup> getGroupsList()
    {
        return new ArrayList<PlaceGroup>(this.groups.values());
    }

    public ExecutionNode getLocalNode()
    {
        return localNode;
    }

    public List<Calendar> getCalendars()
    {
        return new ArrayList<Calendar>(this.calendars.values());
    }
}
