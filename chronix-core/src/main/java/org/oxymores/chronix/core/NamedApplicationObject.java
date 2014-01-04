package org.oxymores.chronix.core;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class NamedApplicationObject extends ApplicationObject
{
    private static final long serialVersionUID = 4596578149244840491L;

    @NotNull
    @Size(min = 1, max = 50)
    protected String name;
    @NotNull
    @Size(min = 1, max = 255)
    protected String description;

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
}
