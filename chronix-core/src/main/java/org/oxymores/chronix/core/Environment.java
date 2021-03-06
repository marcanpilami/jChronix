package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Min;

import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.validation.NetworkCheckConsole;
import org.oxymores.chronix.core.validation.NetworkCheckPnUnicity;

@NetworkCheckConsole
@NetworkCheckPnUnicity
public class Environment extends ChronixObject
{
    private static final long serialVersionUID = -6797237891447380075L;

    static final int currentModelVersion = 1;
    static final int compatibleUpToBackwards = 0;

    @Min(0)
    private int modelVersion = currentModelVersion;

    @Valid
    private Map<UUID, ExecutionNode> nodes = new HashMap<>();
    @Valid
    private Map<UUID, Place> places = new HashMap<>();
    private ExecutionNode console = null;

    ///////////////////////////
    // Places
    public void addPlace(Place place)
    {
        if (!this.places.containsValue(place))
        {
            place.setEnvironment(this);
            this.places.put(place.id, place);
        }
    }

    public void removePlace(Place place)
    {
        this.places.remove(place.id);
        place.setEnvironment(null);
    }

    public Map<UUID, Place> getPlaces()
    {
        HashMap<UUID, Place> res = new HashMap<UUID, Place>();
        res.putAll(this.places);
        return res;
    }

    public List<Place> getPlacesList()
    {
        return new ArrayList<Place>(this.places.values());
    }

    public List<UUID> getPlacesIdList()
    {
        List<UUID> res = new ArrayList<>();
        for (Place p : this.places.values())
        {
            res.add(p.getId());
        }
        return res;
    }

    public Place getPlace(UUID id)
    {
        return this.places.get(id);
    }

    public Place getPlace(String name)
    {
        Place res = null;
        for (Place p : this.places.values())
        {
            if (p.getName().equals(name))
            {
                return p;
            }
        }
        return res;
    }

    ///////////////////////////
    // Nodes
    public void addNode(ExecutionNode o)
    {
        if (!this.nodes.containsValue(o))
        {
            o.setEnvironment(this);
            this.nodes.put(o.id, o);
        }
    }

    public void removeNode(ExecutionNode o)
    {
        this.nodes.remove(o.id);
        o.setEnvironment(null);
    }

    public Map<UUID, ExecutionNode> getNodes()
    {
        HashMap<UUID, ExecutionNode> res = new HashMap<UUID, ExecutionNode>();
        res.putAll(this.nodes);
        return res;
    }

    public List<ExecutionNode> getNodesList()
    {
        return new ArrayList<ExecutionNode>(this.nodes.values());
    }

    public ExecutionNode getNode(UUID id)
    {
        return this.nodes.get(id);
    }

    public ExecutionNode getNode(String name)
    {
        for (ExecutionNode n : this.nodes.values())
        {
            if (n.getName().equals(name))
            {
                return n;
            }
        }
        return null;
    }

    ///////////////////////////
    // Misc.
    public ExecutionNode getConsoleNode()
    {
        if (this.console != null)
        {
            return this.console;
        }
        return this.nodes.values().iterator().next();
    }

    public int getModelVersion()
    {
        return modelVersion;
    }

    public void setModelVersion(int modelVersion)
    {
        this.modelVersion = modelVersion;
    }

    /** Do NOT use is normal operations - use #getConsoleNode instead **/
    public ExecutionNode getConsole()
    {
        return console;
    }

    public void setConsole(ExecutionNode console)
    {
        this.console = console;
    }

}
