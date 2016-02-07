package org.oxymores.chronix.dto;

import java.util.List;

public class DTOEnvironment
{
    private List<DTOPlace> places;
    private List<DTOExecutionNode> nodes;

    public List<DTOPlace> getPlaces()
    {
        return places;
    }

    /**
     * Helper method. Please note that the name is not a unique key - if there are multiple places with the same name, thr first is
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
}
