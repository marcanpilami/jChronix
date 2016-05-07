package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class DTOState implements Serializable
{
    private static final long serialVersionUID = -3105166748835675337L;

    protected UUID id = UUID.randomUUID();

    // Drawings
    private Integer x, y;

    private Boolean parallel = false;

    private Integer warnAfterMn, killAfterMn, maxPipeWaitTime, eventValidityMn;

    private UUID eventSourceId;

    private List<UUID> exclusiveStatesId;

    private UUID runsOnId;

    // Calendar stuff
    private UUID calendarId;
    private int calendarShift = 0;
    private Boolean loopMissedOccurrences = false;
    private Boolean endOfOccurrence = false;
    private Boolean blockIfPreviousFailed = false;

    // private List<DTOToken> tokens; sequences

    public Integer getX()
    {
        return x;
    }

    public DTOState setX(Integer x)
    {
        this.x = x;
        return this;
    }

    public Integer getY()
    {
        return y;
    }

    public DTOState setY(Integer y)
    {
        this.y = y;
        return this;
    }

    public Boolean getParallel()
    {
        return parallel;
    }

    public void setParallel(Boolean parallel)
    {
        this.parallel = parallel;
    }

    public Integer getWarnAfterMn()
    {
        return warnAfterMn;
    }

    public void setWarnAfterMn(Integer warnAfterMn)
    {
        this.warnAfterMn = warnAfterMn;
    }

    public Integer getKillAfterMn()
    {
        return killAfterMn;
    }

    public void setKillAfterMn(Integer killAfterMn)
    {
        this.killAfterMn = killAfterMn;
    }

    public Integer getMaxPipeWaitTime()
    {
        return maxPipeWaitTime;
    }

    public void setMaxPipeWaitTime(Integer maxPipeWaitTime)
    {
        this.maxPipeWaitTime = maxPipeWaitTime;
    }

    public Integer getEventValidityMn()
    {
        return eventValidityMn;
    }

    public void setEventValidityMn(Integer eventValidityMn)
    {
        this.eventValidityMn = eventValidityMn;
    }

    public UUID getEventSourceId()
    {
        return eventSourceId;
    }

    public void setEventSourceId(UUID eventSourceId)
    {
        this.eventSourceId = eventSourceId;
    }

    public List<UUID> getExclusiveStatesId()
    {
        return exclusiveStatesId;
    }

    public void setExclusiveStatesId(List<UUID> exclusiveStatesId)
    {
        this.exclusiveStatesId = exclusiveStatesId;
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public UUID getRunsOnId()
    {
        return runsOnId;
    }

    public void setRunsOnId(UUID runsOnId)
    {
        this.runsOnId = runsOnId;
    }

    public UUID getCalendarId()
    {
        return calendarId;
    }

    public void setCalendarId(UUID calendarId)
    {
        this.calendarId = calendarId;
    }

    public int getCalendarShift()
    {
        return calendarShift;
    }

    public void setCalendarShift(int calendarShift)
    {
        this.calendarShift = calendarShift;
    }

    public Boolean getLoopMissedOccurrences()
    {
        return loopMissedOccurrences;
    }

    public void setLoopMissedOccurrences(Boolean loopMissedOccurrences)
    {
        this.loopMissedOccurrences = loopMissedOccurrences;
    }

    public Boolean getEndOfOccurrence()
    {
        return endOfOccurrence;
    }

    public void setEndOfOccurrence(Boolean endOfOccurrence)
    {
        this.endOfOccurrence = endOfOccurrence;
    }

    public Boolean getBlockIfPreviousFailed()
    {
        return blockIfPreviousFailed;
    }

    public void setBlockIfPreviousFailed(Boolean blockIfPreviousFailed)
    {
        this.blockIfPreviousFailed = blockIfPreviousFailed;
    }

}
