package org.oxymore.chronix.source;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChainEnd;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.engine.modularity.runner.RunResult;

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
    public void deserialize(File sourceFile, EngineCallback cb)
    {
        // Only one chain end for the whole application. No need to put it inside a file.
        DTOChainEnd end = new DTOChainEnd();
        cb.registerSource(end);
    }

    @Override
    public RunResult run(EngineCallback cb, JobDescription jd)
    {
        // This method creates to results: one for itself (we are good!) and one for the parent chain., inside the parent chain scope.

        // Parent result
        RunResult rr = new RunResult();
        rr.returnCode = 0;
        rr.logStart = this.getSourceName();
        rr.id1 = jd.getParentScopeLaunchId(); // Scope is: parent chain launch ID.
        rr.end = jd.getVirtualTimeStart();
        cb.sendRunResult(rr);

        // Self result
        RunResult res = new RunResult();
        res.returnCode = 0;
        res.start = jd.getVirtualTimeStart();
        res.end = res.start;
        return res;
    }

    @Override
    public RunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }

}
