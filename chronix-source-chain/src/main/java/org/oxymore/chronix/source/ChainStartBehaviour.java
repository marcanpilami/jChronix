package org.oxymore.chronix.source;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChainStart;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.engine.modularity.runner.RunResult;

@Component(immediate = true, service = EventSourceBehaviour.class)
public class ChainStartBehaviour extends EventSourceBehaviour
{
    @Override
    public String getSourceName()
    {
        return "chain start";
    }

    @Override
    public String getSourceDescription()
    {
        return "the first item launched when a chain is launched. The first event in a chain claunch is always created by this.";
    }

    @Override
    public void deserialize(File sourceFile, EngineCallback cb)
    {
        // Only one chain start for the whole application. No need to put it inside a file.
        DTOChainStart start = new DTOChainStart();
        cb.registerSource(start);
    }

    @Override
    public RunResult run(EngineCallback cb, JobDescription jd)
    {
        RunResult rr = new RunResult();
        rr.returnCode = 0;
        return rr;
    }

    @Override
    public RunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return run(cb, jd);
    }

}
