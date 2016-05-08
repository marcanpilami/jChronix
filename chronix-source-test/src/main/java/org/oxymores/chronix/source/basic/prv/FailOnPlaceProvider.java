package org.oxymores.chronix.source.basic.prv;

import java.util.ArrayList;
import java.util.List;

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

@Component
public class FailOnPlaceProvider implements EventSourceProvider, RunModeTriggered
{
    @Override
    public String getName()
    {
        return "always OK except if running on a specific place";
    }

    @Override
    public String getDescription()
    {
        return getName();
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(1);
        res.add(new EventSourceField("PLACENAME", "name of the place on which to fail. Case sensitive.", null, true));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        String name = jd.getFields().get("PLACENAME");
        String currentlyRunningOnPlace = jd.getEnvironment().get("CHR_PLACENAME");

        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = currentlyRunningOnPlace.equals(name) ? 1 : 0;
        rr.logStart = "";
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return event.getConditionData1().equals(tr.getGuard1());
    }
}
