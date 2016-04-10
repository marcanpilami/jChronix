package org.oxymores.chronix.source.basic.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.EventSourceField;
import org.oxymores.chronix.api.source2.EventSourceProvider;
import org.oxymores.chronix.api.source2.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;

@Component
public class NoopProvider implements EventSourceProvider, RunModeTriggered
{
    private static transient DTOEventSource _instance = null;
    static final UUID NOOP_ID = UUID.fromString("7852ca09-11bf-47a4-aeba-4adf4f978881");

    @Override
    public String getName()
    {
        return "no-operation";
    }

    @Override
    public String getDescription()
    {
        return "an event source that does nothing. Used as placeholder or for tests.";
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
            _instance = new DTOEventSource(this, "AND", "logical door", NOOP_ID);
        }
        app.addEventSource(_instance);
        return _instance;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "no-operation success";
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}
