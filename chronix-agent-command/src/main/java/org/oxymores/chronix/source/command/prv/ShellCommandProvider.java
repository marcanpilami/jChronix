package org.oxymores.chronix.source.command.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source.OptionAllowsAdditionalFields;
import org.oxymores.chronix.api.source.OptionAllowsParameters;
import org.oxymores.chronix.api.source.RunModeTriggered;

@Component
public class ShellCommandProvider implements EventSourceProvider, RunModeTriggered, OptionAllowsAdditionalFields, OptionAllowsParameters
{
    @Override
    public String getName()
    {
        return "Shell command";
    }

    @Override
    public String getDescription()
    {
        return "a command that can be run against a variety of Unix and Windows shells";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(3);
        res.add(new EventSourceField("runnerCapability", "the type of runner which should run this command", null, true));
        return res;
    }

    @Override
    public EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd)
    {
        // Just send the command to the command agent
        CommandDescription cd = new CommandDescription();
        cd.setLaunchId(jd.getLaunchId());
        cd.setRunnerCapability(jd.getFields().get("runnerCapability"));
        cd.setStoreLogFile(true);
        for (Map.Entry<String, String> prm : jd.getParameters())
        {
            cd.addParameter(prm.getKey(), prm.getValue());
        }
        for (Map.Entry<String, String> prm : jd.getFields().entrySet())
        {
            cd.addPluginParameter(prm.getKey(), prm.getValue());
        }
        cb.sendMessage(cd, "Q." + cb.getNodeName() + ".RUNNER", cb.getResultQueueName());

        // This plugin is always asynchronous.
        return null;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return event.getConditionData1() != null && event.getConditionData1().equals(tr.getGuard1());
    }
}
