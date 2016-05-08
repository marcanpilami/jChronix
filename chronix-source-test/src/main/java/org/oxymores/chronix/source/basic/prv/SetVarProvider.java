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
public class SetVarProvider implements EventSourceProvider, RunModeTriggered
{
    @Override
    public String getName()
    {
        return "sets an environment variable";
    }

    @Override
    public String getDescription()
    {
        return "Sets an environment value that can be used by later jobs in the same context or sub contexts.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(2);
        res.add(new EventSourceField("VARNAME", "name of the variable to set. Case sensitive.", null, true));
        res.add(new EventSourceField("VALUE", "value to set.", null, true));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        String name = jd.getFields().get("VARNAME");
        String value = jd.getFields().get("VALUE");

        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "done";
        rr.newEnvVars.put(name, value);
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}
