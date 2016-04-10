package org.oxymores.chronix.core.engine.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.DTOParameter;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOApplication
{
    private String name;
    private String description;
    private UUID id;
    private boolean active = true;
    private int version = 0;
    private String latestVersionComment = "";

    private List<DTOEventSource> eventSources = new ArrayList<>();
    private List<DTOPlaceGroup> groups = new ArrayList<>();

    private Map<UUID, DTOParameter> sharedParameters = new HashMap<>();

    public DTOPlaceGroup getGroup(String name)
    {
        for (DTOPlaceGroup pg : groups)
        {
            if (pg.getName().equals(name))
            {
                return pg;
            }
        }
        throw new RuntimeException("no group named " + name);
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

    public List<DTOEventSource> getEventSources()
    {
        return eventSources;
    }

    void setEventSources(List<DTOEventSource> eventSources)
    {
        this.eventSources = eventSources;
    }

    public DTOEventSource addEventSource(DTOEventSource source)
    {
        this.eventSources.add(source);
        return source;
    }

    public List<DTOPlaceGroup> getGroups()
    {
        return groups;
    }

    public void setGroups(List<DTOPlaceGroup> groups)
    {
        this.groups = groups;
    }

    public DTOParameter getSharedParameter(UUID id)
    {
        return this.sharedParameters.get(id);
    }
    // private List<DTOParameter> parameters;

}
