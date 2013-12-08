package org.oxymores.chronix.dto;

import java.util.ArrayList;

public class DTOCalendar
{
	public String id, name, description;
	public int alertThreshold;
	public ArrayList<DTOCalendarDay> days;

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

	public int getAlertThreshold()
	{
		return alertThreshold;
	}

	public void setAlertThreshold(int alertThreshold)
	{
		this.alertThreshold = alertThreshold;
	}

	public ArrayList<DTOCalendarDay> getDays()
	{
		return days;
	}

	public void setDays(ArrayList<DTOCalendarDay> days)
	{
		this.days = days;
	}

}
