package org.oxymore.chronix.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChainEnd;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;

@Component(immediate = true, service = EventSourceBehaviour.class)
public class ChainEndBehaviour extends EventSourceBehaviour
{

    @Override
    public String getSourceName()
    {
        return "CHAIN END";
    }

    @Override
    public String getSourceDescription()
    {
        return "A chain must always end by running this source";
    }

    @Override
    public void deserialize(File sourceFile, EventSourceRegistry cb)
    {
        // Only one chain end for the whole application. No need to put it inside a file.
        DTOChainEnd end = new DTOChainEnd();
        cb.registerSource(end);
    }

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

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(DTOChainEnd.class);
        return res;
    }
}
