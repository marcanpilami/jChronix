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
public class SleepProvider implements EventSourceProvider, RunModeTriggered
{
    @Override
    public String getName()
    {
        return "simply waits for a given duration";
    }

    @Override
    public String getDescription()
    {
        return "Thread.sleep as an event source";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(2);
        res.add(new EventSourceField("DURATION", "time to wait for in seconds", null, true));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, final EngineCallback cb, JobDescription jd)
    {
        final int sleep = Integer.parseInt(jd.getFields().get("DURATION"));

        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(sleep * 1000);
                }
                catch (InterruptedException e)
                {
                    EventSourceRunResult res = new EventSourceRunResult();
                    res.logStart = "failed to wait for " + sleep + " seconds " + e.getMessage();
                    res.returnCode = 1;
                    res.success = false;
                    cb.sendRunResult(res);
                    return;
                }
                EventSourceRunResult res = new EventSourceRunResult();
                res.returnCode = 0;
                res.logStart = "waited for " + sleep + " seconds";
                cb.sendRunResult(res);
            }
        }).start();

        return null;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return event.getConditionData1().equals(tr.getGuard1());
    }
}
