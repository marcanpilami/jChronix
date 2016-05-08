package org.oxymores.chronix.core.engine.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOApplication
{
    private String name;
    private String description;
    private UUID id;
    private boolean active = true;
    private int version = 0;
    private String latestVersionComment = "";

    private Map<UUID, DTOEventSource> eventSources = new HashMap<>();
    private Map<UUID, DTOPlaceGroup> groups = new HashMap<>();

    private Map<UUID, DTOParameter> sharedParameters = new HashMap<>();

    public DTOPlaceGroup getGroup(String name)
    {
        for (DTOPlaceGroup pg : groups.values())
        {
            if (pg.getName().equals(name))
            {
                return pg;
            }
        }
        throw new RuntimeException("no group named " + name);
    }

    public DTOApplication addGroup(DTOPlaceGroup gr)
    {
        this.groups.put(gr.getId(), gr);
        return this;
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

    public Collection<DTOEventSource> getEventSources()
    {
        return eventSources.values();
    }

    void setEventSources(Map<UUID, DTOEventSource> eventSources)
    {
        this.eventSources = eventSources;
    }

    /**
     * Adds an new event source to the application. Note that there can only be one source with the same ID so any existing source with an
     * ID equal to the one of the parameter will be replaced.
     * 
     * @param source
     * @return
     */
    public DTOEventSource addEventSource(DTOEventSource source)
    {
        this.eventSources.put(source.getId(), source);
        return source;
    }

    public Collection<DTOPlaceGroup> getGroups()
    {
        return groups.values();
    }

    public void setGroups(Map<UUID, DTOPlaceGroup> groups)
    {
        this.groups = groups;
    }

    public DTOParameter getSharedParameter(UUID id)
    {
        return this.sharedParameters.get(id);
    }
    // private List<DTOParameter> parameters;

}
