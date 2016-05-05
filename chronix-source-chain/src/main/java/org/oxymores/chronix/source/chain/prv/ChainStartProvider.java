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
import org.oxymores.chronix.api.source.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;

@Component(immediate = true)
public class ChainStartProvider implements EventSourceProvider, RunModeTriggered
{
    static final UUID START_ID = UUID.fromString("647594b0-498f-4042-933f-855682095c6c");

    @Override
    public String getName()
    {
        return "chain start";
    }

    @Override
    public String getDescription()
    {
        return "the first item launched when a chain is launched. The first event in a chain claunch is always created by this.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        return new ArrayList<EventSourceField>(0);
    }

    @Override
    public void onNewApplication(DTOApplication newApp)
    {
        new DTOEventSource(this, newApp, "START", "start of a chain", START_ID);
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}