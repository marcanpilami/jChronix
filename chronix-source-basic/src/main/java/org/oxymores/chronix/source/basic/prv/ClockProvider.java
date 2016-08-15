package org.oxymores.chronix.source.basic.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.RunModeExternallyTriggered;

@Component
public class ClockProvider implements EventSourceProvider, RunModeExternallyTriggered
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

        res.add(new EventSourceField("externalData", "a string passed at runtime containing the date to parse", null, false));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.overloadedSequenceOccurrence = getCalendarString(jd);
        return rr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }

    private String getCalendarString(JobDescription jd)
    {
        String regex = jd.getFields().get("regularExpression");
        if (regex == null || regex.isEmpty())
        {
            return null;
        }
        String data = jd.getFields().get("externalData");
        if (data == null || data.isEmpty())
        {
            return null;
        }

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(data);
        if (m.find())
        {
            return m.group(1);
        }
        else
        {
            return null;
        }
    }

}
