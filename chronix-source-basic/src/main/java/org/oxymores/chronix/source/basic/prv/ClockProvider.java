package org.oxymores.chronix.source.basic.prv;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceTimedRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.RunModeTimeTriggered;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.IgnoreDTStartProperty;
import net.fortuna.ical4j.model.property.RRule;

@Component
public class ClockProvider implements EventSourceProvider, RunModeTimeTriggered
{
    @Override
    public String getName()
    {
        return "clock";
    }

    @Override
    public String getDescription()
    {
        return "Will self-trigger according to calendar/clock (based on iCalendar - as in Outlook or Notes meeting planification) rules.";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(3);
        res.add(new EventSourceField("eventBase", "iCal event basic definition, as in ", null, false));
        res.add(new EventSourceField("eventAddRRule", "A pipe separated list of Recurrence Rules. As in: FREQ=DAILY;INTERVAL=2;BYHOUR=3,6;",
                null, false));
        res.add(new EventSourceField("eventExcRRule", "Same as eventAddRRule but used to define exceptions to the rules.", null, false));

        return res;
    }

    // We ask to be recalled at least once every hour. This prevents having to compute recurrences on too long timespans (or to have to
    // cache results).

    @Override
    public EventSourceTimedRunResult run(DTOEventSource source, JobDescription jd)
    {
        EventSourceTimedRunResult esrr = new EventSourceTimedRunResult();

        // Create the iCal object from parameters.
        VEvent evt = new VEvent();
        evt.getProperties().add(new Duration(new Dur(0)));
        evt.getProperties().add(new IgnoreDTStartProperty());

        try
        {
            String rrAdd = jd.getFields().get("eventAddRRule");
            if (rrAdd != null && !rrAdd.isEmpty())
            {
                for (String rr : rrAdd.split("\\|"))
                {
                    evt.getProperties().add(new RRule(rr));
                }
            }
            String rrEx = jd.getFields().get("eventExcRRule");
            if (rrEx != null && !rrEx.isEmpty())
            {
                for (String rr : rrEx.split("\\|"))
                {
                    evt.getProperties().add(new ExRule(new Recur(rr)));
                }
            }
        }
        catch (ParseException z)
        {
            esrr.returnCode = 1;
            esrr.logStart = "invalid configuration";
            esrr.fullerLog = z.getMessage();
            // Next occurrence is not set - no need to come back!
            return esrr;
        }

        // Get all occurrences between vTime and vTime+1 hour (see note above as for why one hour).
        DateTime from = new DateTime(jd.getVirtualTimeStart().toDate());
        DateTime to = new DateTime(jd.getVirtualTimeStart().plusHours(1).toDate());
        evt.getProperties().add(new DtStart(new DateTime(jd.getVirtualTimeStart().minusYears(0).toDate())));
        PeriodList occurrences = evt.calculateRecurrenceSet(new Period(from, to));
        System.out.println(occurrences);
        System.out.println("RRRRRRRR");
        System.out.println(evt);

        // Find the correct occurrence and next occurrences, if any
        org.joda.time.DateTime nextCall = jd.getVirtualTimeStart().plusHours(1); // Call us back at most one hour later.
        for (Object o : occurrences)
        {
            Period p = (Period) o;
            System.out.println(p);
            org.joda.time.DateTime start = new org.joda.time.DateTime(p.getStart());
            if (start.equals(jd.getVirtualTimeStart()))
            {
                // Clock should create an event at this point in virtual time.
                esrr.returnCode = 0;
            }
            else
            {
                // This is a next possible call.
                if (start.isBefore(nextCall))
                {
                    nextCall = start;
                }
            }
        }
        esrr.callMeBackAt = nextCall;

        if (esrr.returnCode == null || esrr.returnCode != 0)
        {
            esrr.logStart = "clock does not  need to run at " + jd.getVirtualTimeStart();
            esrr.returnCode = 2;
        }

        return esrr;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return tr.getGuard1().equals(event.getConditionData1());
    }
}
