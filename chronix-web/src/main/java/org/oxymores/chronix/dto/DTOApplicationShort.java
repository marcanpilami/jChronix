package org.oxymores.chronix.dto;

public class DTOApplicationShort
{
    private String id, name, description;
    private int version;

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

    public int getVersion()
    {
        return this.version;
    }

    public void setVersion(int v)
    {
        this.version = v;
    }
}
