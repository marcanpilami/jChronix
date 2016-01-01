package org.oxymores.chronix.dto;

import java.util.HashMap;
import java.util.Map;

public class DTOShellCommand
{
    private String id;
    private String command, name, description;
    private Map<String, String> pluginParameters = new HashMap<>();

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        this.command = command;
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

    public Map<String, String> getPluginParameters()
    {
        return pluginParameters;
    }

    public void setPluginParameters(Map<String, String> pluginParameters)
    {
        this.pluginParameters = pluginParameters;
    }
}
