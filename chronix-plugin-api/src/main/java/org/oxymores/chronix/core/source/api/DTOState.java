package org.oxymores.chronix.core.source.api;

import java.util.List;
import java.util.UUID;

public class DTOState
{
    protected UUID id = UUID.randomUUID();

    // Drawings
    protected Integer x, y;

    protected Boolean parallel = false;

    protected Integer warnAfterMn, killAfterMn, maxPipeWaitTime, eventValidityMn;

    protected UUID eventSourceId;

    protected List<UUID> exclusiveStatesId;

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

}
