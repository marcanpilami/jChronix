package org.oxymores.chronix.source.chain.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.RunModeDisabled;
import org.oxymores.chronix.api.source.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;

@Component
public class ChainEndProvider implements EventSourceProvider, RunModeTriggered, RunModeDisabled
{
    static final UUID END_ID = UUID.fromString("8235272c-b78d-4350-a887-aed0dcdfb215");

    @Override
    public String getName()
    {
        return "CHAIN END";
    }

    @Override
    public String getDescription()
    {
        return "A chain must always end by running this source";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        return new ArrayList<EventSourceField>(0);
    }

    @Override
    public void onNewApplication(DTOApplication newApp)
    {
        new DTOEventSource(this, newApp, "END", "end of chain", END_ID);
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        // This method creates to results: one for itself (we are good!) and one for the parent chain., inside the parent chain scope.

        // Parent result
        if (jd.getParentContainerLaunchId() != null)
        {
            EventSourceRunResult rr = new EventSourceRunResult();
            rr.returnCode = 0;
            rr.overloadedLaunchId = jd.getParentContainerLaunchId(); // Scope is: parent chain launch ID.
            cb.sendRunResult(rr);
        }

        // Self result
        EventSourceRunResult res = new EventSourceRunResult();
        res.returnCode = 0;
        return res;
    }

    @Override
    public EventSourceRunResult runDisabled(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        // Even when disabled, it should run.
        return this.run(source, cb, jd);
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}
