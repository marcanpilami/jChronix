package org.oxymores.chronix.source.chain.dto;

import java.util.UUID;

import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceOptionCannotEmit;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.EventSourceTriggered;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.source.chain.prv.ChainEndBehaviour;

public class ChainEnd extends EventSourceTriggered implements EventSourceOptionCannotEmit
{
    private static final long serialVersionUID = -3859771632110912194L;
    static final UUID END_ID = UUID.fromString("8235272c-b78d-4350-a887-aed0dcdfb215");

    public ChainEnd()
    {
        this.name = "END";
        this.description = "end of chain";
        this.id = END_ID;
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ChainEndBehaviour.class;
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUN
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        // This method creates to results: one for itself (we are good!) and one for the parent chain., inside the parent chain scope.

        // Parent result
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.overloadedScopeId = jd.getParentScopeLaunchId(); // Scope is: parent chain launch ID.
        rr.end = jd.getVirtualTimeStart();
        cb.sendRunResult(rr);

        // Self result
        EventSourceRunResult res = new EventSourceRunResult();
        res.end = jd.getVirtualTimeStart(); // Does not take any time.
        res.returnCode = 0;
        return res;
    }

    @Override
    public EventSourceRunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }
}
