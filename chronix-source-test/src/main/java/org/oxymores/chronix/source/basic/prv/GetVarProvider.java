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
public class GetVarProvider implements EventSourceProvider, RunModeTriggered
{
    @Override
    public String getName()
    {
        return "write environment variable value to log.";
    }

    @Override
    public String getDescription()
    {
        return "Simply returns the value of an environment variable inside its log. Fails if variable not defined.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(1);
        res.add(new EventSourceField("VARNAME", "name of the variable. Case sensitive.", null, true));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        String name = jd.getFields().get("VARNAME");
        String value = jd.getEnvironment().get(name);

        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = (value == null ? 1 : 0);
        rr.logStart = (value == null ? "variable not defined!" : value);
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return event.getConditionData1().equals(tr.getGuard1());
    }
}
