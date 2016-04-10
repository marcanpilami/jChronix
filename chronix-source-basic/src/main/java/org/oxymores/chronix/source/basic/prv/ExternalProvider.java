package org.oxymores.chronix.source.basic.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

@Component
public class ExternalProvider implements EventSourceProvider, RunModeTriggered
{
    @Override
    public String getName()
    {
        return "external";
    }

    @Override
    public String getDescription()
    {
        return "Represents an incoming event from an external system that is provided through a command line call.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(3);
        res.add(new EventSourceField("machineRestriction", "hostname allowed to send event", null, false));
        res.add(new EventSourceField("accountRestriction", "user account allowed to send event", null, false));
        res.add(new EventSourceField("regularExpression", "regular expression to parse date from event data", null, false));
        return res;
    }

    @Override
    public DTOEventSource newInstance(String name, String description, DTOApplication app, Object... field)
    {
        DTOEventSource res = new DTOEventSource(this, name, description, null);
        // TODO: parameters.
        return res;
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

    /*
     * private String getCalendarString(DTOEventSource dto, String data) { if (dto.get regularExpression == null ||
     * "".equals(regularExpression)) { return null; }
     * 
     * Pattern p = Pattern.compile(regularExpression); Matcher m = p.matcher(data);
     * 
     * if (m.find()) { return m.group(1); } else { return null; } }
     */
}
