package org.oxymores.chronix.dto;

import java.util.List;

public class DTONetwork
{
    private List<DTOPlace> places;
    private List<DTOExecutionNode> nodes;

    public List<DTOPlace> getPlaces()
    {
        return places;
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
