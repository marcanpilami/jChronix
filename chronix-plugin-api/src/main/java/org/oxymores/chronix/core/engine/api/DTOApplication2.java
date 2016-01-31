package org.oxymores.chronix.core.engine.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTO;

public class DTOApplication2
{
    private String name;
    private String description;
    private UUID id;
    private boolean active = true;
    private int version = 0;
    private String latestVersionComment = "";

    private List<DTO> eventSources = new ArrayList<>();

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

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getLatestVersionComment()
    {
        return latestVersionComment;
    }

    public void setLatestVersionComment(String latestVersionComment)
    {
        this.latestVersionComment = latestVersionComment;
    }

    public List<DTO> getEventSources()
    {
        return eventSources;
    }

    public void setEventSources(List<DTO> eventSources)
    {
        this.eventSources = eventSources;
    }

    public void addEventSource(DTO source)
    {
        this.eventSources.add(source);
    }

    // private List<DTOPlaceGroup> groups;
    // private List<DTOParameter> parameters;

}
