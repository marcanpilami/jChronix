package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;

public class DTOEnvironment
{
    private List<DTOPlace> places;
    private List<DTOExecutionNode> nodes;

    /**
     * All the places known. (copy of the actual list)
     */
    public List<DTOPlace> getPlaces()
    {
        return new ArrayList<>(places);
    }

    /**
     * Add a new place to the environment. Note that places should be unique (by ID).
     */
    public DTOEnvironment addPlace(DTOPlace place)
    {
        this.places.add(place);
        return this;
    }

    /**
     * Helper method. Please note that the name is not a unique key - if there are multiple places with the same name, the first is
     * returned.
     */
    public DTOPlace getPlace(String name)
    {
        for (DTOPlace p : places)
        {
            if (p.getName().equals(name))
            {
                return p;
            }
        }
        throw new RuntimeException("no place named " + name);
    }

    public void setPlaces(List<DTOPlace> places)
    {
        this.places = places;
    }

    public List<DTOExecutionNode> getNodes()
    {
        return nodes;
    }

    public void setNodes(List<DTOExecutionNode> nodes)
    {
        this.nodes = nodes;
    }

    /**
     * Helper method. Please note that the name is not a unique key - if there are multiple nodes with the same name, the first is returned.
     */
    public DTOExecutionNode getExecutionNode(String name)
    {
        for (DTOExecutionNode p : nodes)
        {
            if (p.getName().equals(name))
            {
                return p;
            }
        }
        throw new RuntimeException("no node named " + name);
    }
}
