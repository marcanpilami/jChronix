package org.oxymores.chronix.source.chain.prv;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.DTOEventSourceContainer;
import org.oxymores.chronix.api.source2.EventSourceField;
import org.oxymores.chronix.api.source2.EventSourceProvider;
import org.oxymores.chronix.api.source2.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.dto.DTOPlaceGroup;

@Component
public class ChainProvider implements EventSourceProvider, RunModeTriggered
{
    ///////////////////////////////////////////////////////////////////////////
    // Needed providers (do not use OSGi here - no need)
    ///////////////////////////////////////////////////////////////////////////

    private static ChainStartProvider start = new ChainStartProvider();
    private static ChainEndProvider end = new ChainEndProvider();

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
    public DTOEventSource newInstance(String name, String description, DTOApplication app, Object... parameters)
    {
        if (parameters == null || parameters.length != 1 || !(parameters[0] instanceof DTOPlaceGroup))
        {
            throw new IllegalArgumentException("newInstance for chains takes a single parameter of type DTOPlaceGroup");
        }
        DTOPlaceGroup runsOn = (DTOPlaceGroup) parameters[0];
        DTOEventSourceContainer res = new DTOEventSourceContainer(this, name, description, null);

        res.addState(start.newInstance("", "", app), runsOn).setX(50).setY(50);
        res.addState(end.newInstance("", "", app), runsOn).setX(50).setY(200);

        app.addEventSource(res);
        return res;
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
