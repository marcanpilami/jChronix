package org.oxymores.chronix.source.basic.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTOEvent;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.source.basic.reg.FailureBehaviour;

public class Failure extends EventSourceTriggered
{
    private static final long serialVersionUID = -1686612431751399022L;

    @Override
    public UUID getId()
    {
        return UUID.fromString("7852ca09-11bf-47a4-aeba-4adf4f979881");
    }

    @Override
    public String getName()
    {
        return "FAIL";
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return FailureBehaviour.class;
    }

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult r = new EventSourceRunResult();
        r.returnCode = 1;
        r.logStart = "failure failed!";
        return r;
    }

    @Override
    public boolean isTransitionPossible(DTOTransition tr, DTOEvent event)
    {
        return false;
    }
}
