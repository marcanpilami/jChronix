package org.oxymores.chronix.source.chain.prv;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.dto.DTOPlaceGroup;

@Component
public class ChainProvider implements EventSourceProvider, RunModeTriggered
{
    ///////////////////////////////////////////////////////////////////////////
    // Identity
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public String getName()
    {
        return "chain";
    }

    @Override
    public String getDescription()
    {
        return "a reusable piece of production plan";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        return new ArrayList<>(0);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Run
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        if (!(source instanceof DTOEventSourceContainer))
        {
            throw new IllegalArgumentException("given event source was not a container");
        }
        DTOEventSourceContainer s = (DTOEventSourceContainer) source;

        // Enqueue start (in a new context)
        cb.launchState(this.getStart(s));

        // A chain only starts its own start event source. The actual RunResult is sent by the end source, so all we need here is to return
        // at once.
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Factory
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void onNewSource(DTOEventSource source, DTOApplication app)
    {
        DTOEventSourceContainer s = (DTOEventSourceContainer) source;
        DTOPlaceGroup pg = app.getGroups().iterator().next();
        s.addState(getSingletonSource(app, ChainStartProvider.class), pg).setX(50).setY(50);
        s.addState(getSingletonSource(app, ChainEndProvider.class), pg).setX(50).setY(200);
    }

    private DTOEventSource getSingletonSource(DTOApplication app, Class<? extends EventSourceProvider> cl)
    {
        for (DTOEventSource s : app.getEventSources())
        {
            if (s.getBehaviourClassName().equals(cl.getCanonicalName()))
            {
                return s;
            }
        }
        throw new RuntimeException("no singleton found for type " + cl.getCanonicalName());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Helpers for chain manipulation
    ///////////////////////////////////////////////////////////////////////////

    public DTOState getStart(DTOEventSourceContainer source)
    {
        for (DTOState s : source.getContainedStates())
        {
            if (s.getEventSourceId().equals(ChainStartProvider.START_ID))
            {
                return s;
            }
        }
        throw new IllegalStateException("chain has no start");
    }

    public DTOState getEnd(DTOEventSourceContainer source)
    {
        for (DTOState s : source.getContainedStates())
        {
            if (s.getEventSourceId().equals(ChainEndProvider.END_ID))
            {
                return s;
            }
        }
        throw new IllegalStateException("chain has no end");
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis: there is never any interesting data inside chain events
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}
