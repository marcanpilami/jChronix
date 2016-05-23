package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.dto.DTOFunctionalSequence;

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
    private boolean moveCalendarForwardOnSuccess = false;
    private Boolean loopMissedOccurrences = false;
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

    public DTOState setParallel(Boolean parallel)
    {
        this.parallel = parallel;
        return this;
    }

    public Integer getWarnAfterMn()
    {
        return warnAfterMn;
    }

    public DTOState setWarnAfterMn(Integer warnAfterMn)
    {
        this.warnAfterMn = warnAfterMn;
        return this;
    }

    public Integer getKillAfterMn()
    {
        return killAfterMn;
    }

    public DTOState setKillAfterMn(Integer killAfterMn)
    {
        this.killAfterMn = killAfterMn;
        return this;
    }

    public Integer getMaxPipeWaitTime()
    {
        return maxPipeWaitTime;
    }

    public DTOState setMaxPipeWaitTime(Integer maxPipeWaitTime)
    {
        this.maxPipeWaitTime = maxPipeWaitTime;
        return this;
    }

    public Integer getEventValidityMn()
    {
        return eventValidityMn;
    }

    public DTOState setEventValidityMn(Integer eventValidityMn)
    {
        this.eventValidityMn = eventValidityMn;
        return this;
    }

    public UUID getEventSourceId()
    {
        return eventSourceId;
    }

    protected DTOState setEventSourceId(UUID eventSourceId)
    {
        this.eventSourceId = eventSourceId;
        return this;
    }

    public List<UUID> getExclusiveStatesId()
    {
        return exclusiveStatesId;
    }

    void setExclusiveStatesId(List<UUID> exclusiveStatesId)
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

    public DTOState setRunsOnId(UUID runsOnId)
    {
        this.runsOnId = runsOnId;
        return this;
    }

    public UUID getCalendarId()
    {
        return calendarId;
    }

    public DTOState setCalendarId(UUID calendarId)
    {
        this.calendarId = calendarId;
        return this;
    }

    /**
     * Shortcut for {@link #setCalendarId(UUID)}.
     */
    public DTOState setCalendar(DTOFunctionalSequence seq)
    {
        this.setCalendarId(seq.getId());
        return this;
    }

    /**
     * Shift: -1 means that the State will run at D-1 when the reference is at D. Therefore it should stop one occurrence before the others.
     */
    public int getCalendarShift()
    {
        return calendarShift;
    }

    /**
     * See {@link #getCalendarShift()}
     */
    public DTOState setCalendarShift(int calendarShift)
    {
        this.calendarShift = calendarShift;
        return this;
    }

    public Boolean getLoopMissedOccurrences()
    {
        return loopMissedOccurrences;
    }

    public DTOState setLoopMissedOccurrences(Boolean loopMissedOccurrences)
    {
        this.loopMissedOccurrences = loopMissedOccurrences;
        return this;
    }

    public Boolean getBlockIfPreviousFailed()
    {
        return blockIfPreviousFailed;
    }

    public DTOState setBlockIfPreviousFailed(Boolean blockIfPreviousFailed)
    {
        this.blockIfPreviousFailed = blockIfPreviousFailed;
        return this;
    }

    public boolean isMoveCalendarForwardOnSuccess()
    {
        return moveCalendarForwardOnSuccess;
    }

    public DTOState setMoveCalendarForwardOnSuccess(boolean moveCalendarForwardOnSuccess)
    {
        this.moveCalendarForwardOnSuccess = moveCalendarForwardOnSuccess;
        return this;
    }
}
