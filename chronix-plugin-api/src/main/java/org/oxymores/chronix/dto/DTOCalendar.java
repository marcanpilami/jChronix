package org.oxymores.chronix.dto;

import java.util.List;

public class DTOCalendar
{
    private String id, name, description;
    private int alertThreshold;
    private List<DTOCalendarDay> days;

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

    public List<DTOCalendarDay> getDays()
    {
        return days;
    }

    public void setDays(List<DTOCalendarDay> days)
    {
        this.days = days;
    }

}
