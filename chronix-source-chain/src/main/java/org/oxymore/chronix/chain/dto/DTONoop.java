package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymore.chronix.source.NoopBehaviour;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;

public class DTONoop extends EventSourceTriggered
{
    private static final long serialVersionUID = -1686612421751399022L;

    @Override
    public UUID getId()
    {
        return UUID.fromString("7852ca09-11bf-47a4-aeba-4adf4f978881");
    }

    @Override
    public String getName()
    {
        return "no-op";
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return NoopBehaviour.class;
    }

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult r = new EventSourceRunResult();
        r.returnCode = 0;
        r.logStart = "no-operation success";
        return r;
    }

}
