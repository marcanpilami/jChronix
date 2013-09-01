package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.UUID;

public class DTOApplication
{
	public String name;
	public String description;
	public String id;
	public boolean active = true;

	public ArrayList<DTOPlace> places;
	public ArrayList<DTOPlaceGroup> groups;
	public ArrayList<DTOParameter> parameters;
	public ArrayList<DTOChain> chains;
	public ArrayList<DTOShellCommand> shells;
	public ArrayList<DTOExecutionNode> nodes;
	public ArrayList<DTOClock> clocks;
	public ArrayList<DTORRule> rrules;

	public ArrayList<DTOClock> getClocks()
	{
		return clocks;
	}

	public void setClocks(ArrayList<DTOClock> clocks)
	{
		this.clocks = clocks;
	}

	public ArrayList<DTORRule> getRrules()
	{
		return rrules;
	}

	public void setRrules(ArrayList<DTORRule> rrules)
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

	public ArrayList<DTOPlace> getPlaces()
	{
		return places;
	}

	public void setPlaces(ArrayList<DTOPlace> places)
	{
		this.places = places;
	}

	public ArrayList<DTOPlaceGroup> getGroups()
	{
		return groups;
	}

	public void setGroups(ArrayList<DTOPlaceGroup> groups)
	{
		this.groups = groups;
	}

	public ArrayList<DTOParameter> getParameters()
	{
		return parameters;
	}

	public void setParameters(ArrayList<DTOParameter> parameters)
	{
		this.parameters = parameters;
	}

	public ArrayList<DTOChain> getChains()
	{
		return chains;
	}

	public void setChains(ArrayList<DTOChain> chains)
	{
		this.chains = chains;
	}

	public ArrayList<DTOShellCommand> getShells()
	{
		return shells;
	}

	public void setShells(ArrayList<DTOShellCommand> shells)
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

	public ArrayList<DTOExecutionNode> getNodes()
	{
		return nodes;
	}

	public void setNodes(ArrayList<DTOExecutionNode> nodes)
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

}
