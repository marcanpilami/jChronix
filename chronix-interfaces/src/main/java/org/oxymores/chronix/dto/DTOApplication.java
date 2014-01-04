package org.oxymores.chronix.dto;

import java.util.List;
import java.util.UUID;

public class DTOApplication
{
    private String name;
    private String description;
    private String id;
    private boolean active = true;

    private List<DTOPlace> places;
    private List<DTOPlaceGroup> groups;
    private List<DTOParameter> parameters;
    private List<DTOChain> chains;
    private List<DTOShellCommand> shells;
    private List<DTOExecutionNode> nodes;
    private List<DTOClock> clocks;
    private List<DTORRule> rrules;
    private List<DTOExternal> externals;
    private List<DTONextOccurrence> calnexts;
    private List<DTOCalendar> calendars;

    private String andId, orId, startId, endId;

    public List<DTOClock> getClocks()
    {
        return clocks;
    }

    public void setClocks(List<DTOClock> clocks)
    {
        this.clocks = clocks;
    }

    public List<DTORRule> getRrules()
    {
        return rrules;
    }

    public void setRrules(List<DTORRule> rrules)
    {
        this.rrules = rrules;
    }

    protected UUID marsu;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public List<DTOPlace> getPlaces()
    {
        return places;
    }

    public void setPlaces(List<DTOPlace> places)
    {
        this.places = places;
    }

    public List<DTOPlaceGroup> getGroups()
    {
        return groups;
    }

    public void setGroups(List<DTOPlaceGroup> groups)
    {
        this.groups = groups;
    }

    public List<DTOParameter> getParameters()
    {
        return parameters;
    }

    public void setParameters(List<DTOParameter> parameters)
    {
        this.parameters = parameters;
    }

    public List<DTOChain> getChains()
    {
        return chains;
    }

    public void setChains(List<DTOChain> chains)
    {
        this.chains = chains;
    }

    public List<DTOShellCommand> getShells()
    {
        return shells;
    }

    public void setShells(List<DTOShellCommand> shells)
    {
        this.shells = shells;
    }

    public UUID getMarsu()
    {
        return marsu;
    }

    public void setMarsu(UUID marsu)
    {
        this.marsu = marsu;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<DTOExecutionNode> getNodes()
    {
        return nodes;
    }

    public void setNodes(List<DTOExecutionNode> nodes)
    {
        this.nodes = nodes;
    }

    public boolean isActive()
    {
        return active;
    }

    public void setActive(boolean active)
    {
        this.active = active;
    }

    public List<DTOExternal> getExternals()
    {
        return externals;
    }

    public void setExternals(List<DTOExternal> externals)
    {
        this.externals = externals;
    }

    public String getAndId()
    {
        return andId;
    }

    public void setAndId(String andId)
    {
        this.andId = andId;
    }

    public String getOrId()
    {
        return orId;
    }

    public void setOrId(String orId)
    {
        this.orId = orId;
    }

    public String getStartId()
    {
        return startId;
    }

    public void setStartId(String startId)
    {
        this.startId = startId;
    }

    public String getEndId()
    {
        return endId;
    }

    public void setEndId(String endId)
    {
        this.endId = endId;
    }

    public List<DTONextOccurrence> getCalnexts()
    {
        return calnexts;
    }

    public void setCalnexts(List<DTONextOccurrence> calnexts)
    {
        this.calnexts = calnexts;
    }

    public List<DTOCalendar> getCalendars()
    {
        return calendars;
    }

    public void setCalendars(List<DTOCalendar> calendars)
    {
        this.calendars = calendars;
    }

}
