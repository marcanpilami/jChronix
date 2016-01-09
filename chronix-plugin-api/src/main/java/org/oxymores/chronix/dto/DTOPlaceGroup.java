package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOPlaceGroup
{
    private String id, name, description;
    private List<String> places = new ArrayList<String>();

    public void addPlace(UUID id)
    {
        this.places.add(id.toString());
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<String> getPlaces()
    {
        return places;
    }

    void setPlaces(List<String> places)
    {
        this.places = places;
    }
}
