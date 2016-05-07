package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.UUID;

public class DTOTransition implements Serializable
{
    private static final long serialVersionUID = 1992165786923690138L;

    private UUID from, to;
    private Integer guard1;
    private String guard2, guard3;
    private UUID guard4;
    private UUID id = UUID.randomUUID();
    private boolean calendarAware;
    private Integer calendarShift;

    public UUID getFrom()
    {
        return from;
    }

    public void setFrom(UUID from)
    {
        this.from = from;
    }

    public UUID getTo()
    {
        return to;
    }

    public void setTo(UUID to)
    {
        this.to = to;
    }

    public Integer getGuard1()
    {
        return guard1;
    }

    public void setGuard1(Integer guard1)
    {
        this.guard1 = guard1;
    }

    public String getGuard2()
    {
        return guard2;
    }

    public void setGuard2(String guard2)
    {
        this.guard2 = guard2;
    }

    public String getGuard3()
    {
        return guard3;
    }

    public void setGuard3(String guard3)
    {
        this.guard3 = guard3;
    }

    public UUID getGuard4()
    {
        return guard4;
    }

    public void setGuard4(UUID guard4)
    {
        this.guard4 = guard4;
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public boolean isCalendarAware()
    {
        return calendarAware;
    }

    public void setCalendarAware(boolean calendarAware)
    {
        this.calendarAware = calendarAware;
    }

    public Integer getCalendarShift()
    {
        return calendarShift;
    }

    public void setCalendarShift(Integer calendarShift)
    {
        this.calendarShift = calendarShift;
    }
}
