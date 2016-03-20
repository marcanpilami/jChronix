package org.oxymores.chronix.source.basic.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceOptionOr;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.source.basic.prv.OrBehaviour;

public class Or extends EventSourceTriggered implements EventSourceOptionOr
{
    private static final long serialVersionUID = -1686612421751399022L;

    public Or()
    {
        this.id = UUID.fromString("152cf589-f0ca-42ab-b25a-ffc1d03fd579");
        this.name = "OR";
        this.description = "logical door OR";
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return OrBehaviour.class;
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
