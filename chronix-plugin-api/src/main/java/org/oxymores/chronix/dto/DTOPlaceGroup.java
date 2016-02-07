package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOPlaceGroup
{
    private UUID id;
    private String name, description;
    private List<UUID> places = new ArrayList<UUID>();

    /*public void addPlace(UUID id)
    {
        this.places.add(id);
    }*/

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
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

    /*public List<UUID> getPlaces()
    {
        return places;
    }*/

    /*void setPlaces(List<UUID> places)
    {
        this.places = places;
    }*/
}
