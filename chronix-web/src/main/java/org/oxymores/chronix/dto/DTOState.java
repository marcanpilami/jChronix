package org.oxymores.chronix.dto;

public class DTOState
{
    private String id, representsId, runsOnId, calendarId;
    private Integer x, y;
    private String label, runsOnName;
    private Integer WarnAfterMn, KillAfterMn, MaxPipeWaitTime, EventValidityMn;
    private Integer calendarShift = 0;
    private boolean parallel;
    private boolean canReceiveLink = true, canEmitLinks = true, canBeRemoved = true, canReceiveMultipleLinks = false, isStart = false,
            isEnd = false, isAnd = false, isOr = false;

    public String getRunsOnName()
    {
        return runsOnName;
    }

    public void setRunsOnName(String runsOnName)
    {
        this.runsOnName = runsOnName;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    public Integer getWarnAfterMn()
    {
        return WarnAfterMn;
    }

    public void setWarnAfterMn(Integer warnAfterMn)
    {
        WarnAfterMn = warnAfterMn;
    }

    public Integer getKillAfterMn()
    {
        return KillAfterMn;
    }

    public void setKillAfterMn(Integer killAfterMn)
    {
        KillAfterMn = killAfterMn;
    }

    public Integer getMaxPipeWaitTime()
    {
        return MaxPipeWaitTime;
    }

    public void setMaxPipeWaitTime(Integer maxPipeWaitTime)
    {
        MaxPipeWaitTime = maxPipeWaitTime;
    }

    public Integer getEventValidityMn()
    {
        return EventValidityMn;
    }

    public void setEventValidityMn(Integer eventValidityMn)
    {
        EventValidityMn = eventValidityMn;
    }

    public Integer getX()
    {
        return x;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }

    public Integer getY()
    {
        return y;
    }

    public void setY(Integer y)
    {
        this.y = y;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getRepresentsId()
    {
        return representsId;
    }

    public void setRepresentsId(String representsId)
    {
        this.representsId = representsId;
    }

    public String getRunsOnId()
    {
        return runsOnId;
    }

    public void setRunsOnId(String runsOnId)
    {
        this.runsOnId = runsOnId;
    }

    public boolean isCanReceiveLink()
    {
        return canReceiveLink;
    }

    public void setCanReceiveLink(boolean canReceiveLink)
    {
        this.canReceiveLink = canReceiveLink;
    }

    public boolean isCanEmitLinks()
    {
        return canEmitLinks;
    }

    public void setCanEmitLinks(boolean canEmitLinks)
    {
        this.canEmitLinks = canEmitLinks;
    }

    public boolean isCanBeRemoved()
    {
        return canBeRemoved;
    }

    public void setCanBeRemoved(boolean canBeRemoved)
    {
        this.canBeRemoved = canBeRemoved;
    }

    public boolean isCanReceiveMultipleLinks()
    {
        return canReceiveMultipleLinks;
    }

    public void setCanReceiveMultipleLinks(boolean canReceiveMultipleLinks)
    {
        this.canReceiveMultipleLinks = canReceiveMultipleLinks;
    }

    public boolean isStart()
    {
        return isStart;
    }

    public void setStart(boolean isStart)
    {
        this.isStart = isStart;
    }

    public boolean isEnd()
    {
        return isEnd;
    }

    public void setEnd(boolean isEnd)
    {
        this.isEnd = isEnd;
    }

    public boolean isAnd()
    {
        return isAnd;
    }

    public void setAnd(boolean isAnd)
    {
        this.isAnd = isAnd;
    }

    public boolean isOr()
    {
        return isOr;
    }

    public void setOr(boolean isOr)
    {
        this.isOr = isOr;
    }

    public boolean isParallel()
    {
        return parallel;
    }

    public void setParallel(boolean parallel)
    {
        this.parallel = parallel;
    }

    public String getCalendarId()
    {
        return calendarId;
    }

    public void setCalendarId(String calendarId)
    {
        this.calendarId = calendarId;
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
