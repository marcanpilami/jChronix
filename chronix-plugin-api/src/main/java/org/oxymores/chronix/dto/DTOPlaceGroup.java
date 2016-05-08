package org.oxymores.chronix.dto;

import java.util.UUID;

public class DTOPlaceGroup
{
    private UUID id;
    private String name, description;

    public DTOPlaceGroup(String name, String description)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();
    }

    public DTOPlaceGroup(String name, String description, UUID id)
    {
        this(name, description);
        this.id = id;
    }

    public UUID getId()
    {
        return id;
    }

    protected void setId(UUID id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    protected void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }
}
