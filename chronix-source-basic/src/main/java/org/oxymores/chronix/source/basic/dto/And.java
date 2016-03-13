package org.oxymores.chronix.source.basic.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceOptionAnd;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.source.basic.reg.AndBehaviour;

public class And extends EventSourceTriggered implements EventSourceOptionAnd
{
    private static final long serialVersionUID = -1686612421751399022L;

    public And()
    {
        this.id = UUID.fromString("152cf589-f0ca-42ab-b25a-ffc1d03fd577");
        this.name = "AND";
        this.description = "logical door AND";
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return AndBehaviour.class;
    }

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult r = new EventSourceRunResult();
        r.returnCode = 0;
        r.logStart = "";
        return r;
    }

    @Override
    public EventSourceRunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }

    @Override
    public EventSourceRunResult runForceOk(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }
}
