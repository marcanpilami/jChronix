package org.oxymores.chronix.source.basic.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceOptionOr;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.EventSourceField;
import org.oxymores.chronix.api.source2.EventSourceProvider;
import org.oxymores.chronix.api.source2.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;

@Component
public class OrProvider implements EventSourceProvider, RunModeTriggered, EventSourceOptionOr
{
    private static transient DTOEventSource _instance = null;
    static final UUID OR_ID = UUID.fromString("152cf589-f0ca-42ab-b25a-ffc1d03fd579");

    @Override
    public String getName()
    {
        return "or";
    }

    @Override
    public String getDescription()
    {
        return "an event source that does nothing by itself - it is simply a logical door that does an OR between all incoming transitions.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        return new ArrayList<EventSourceField>(0);
    }

    @Override
    public DTOEventSource newInstance(String name, String description, DTOApplication app, Object... field)
    {
        if (_instance == null)
        {
            // Not synchronised - doubles are not a problem.
            _instance = new DTOEventSource(this, "OR", "logical door", OR_ID);
        }
        app.addEventSource(_instance);
        return _instance;
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
