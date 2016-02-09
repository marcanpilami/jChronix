package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;

public class DTOChainStart extends EventSource
{
    private static final long serialVersionUID = -5628734383363442943L;
    static final UUID START_ID = UUID.fromString("647594b0-498f-4042-933f-855682095c6c");

    @Override
    public UUID getId()
    {
        return START_ID;
    }

    @Override
    public String getName()
    {
        return "START";
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        return rr;
    }

    @Override
    public EventSourceRunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }
}
