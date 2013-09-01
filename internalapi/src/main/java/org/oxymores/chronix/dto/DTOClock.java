package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.Date;

public class DTOClock
{
	public String name;
	public String description;
	public String id;

	public ArrayList<Date> nextOccurrences;
	public ArrayList<String> rulesADD, rulesEXC;

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

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public ArrayList<Date> getNextOccurrences()
	{
		return nextOccurrences;
	}

	public void setNextOccurrences(ArrayList<Date> nextOccurrences)
	{
		this.nextOccurrences = nextOccurrences;
	}

	public ArrayList<String> getRulesADD()
	{
		return rulesADD;
	}

	public void setRulesADD(ArrayList<String> rulesADD)
	{
		this.rulesADD = rulesADD;
	}

	public ArrayList<String> getRulesEXC()
	{
		return rulesEXC;
	}

	public void setRulesEXC(ArrayList<String> rulesEXC)
	{
		this.rulesEXC = rulesEXC;
	}
}
