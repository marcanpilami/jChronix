package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymore.chronix.source.ChainStartBehaviour;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceOptionCannotReceive;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;

public class DTOChainStart extends EventSourceTriggered implements EventSourceOptionCannotReceive
{
    private static final long serialVersionUID = -5628734383363442943L;
    static final UUID START_ID = UUID.fromString("647594b0-498f-4042-933f-855682095c6c");

    public DTOChainStart()
    {
        this.name = "START";
        this.description = "start of a chain";
        this.id = START_ID;
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ChainStartBehaviour.class;
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
