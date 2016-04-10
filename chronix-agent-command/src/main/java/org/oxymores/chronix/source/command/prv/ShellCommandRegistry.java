package org.oxymores.chronix.source.command.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.api.source2.DTOEventSource;
import org.oxymores.chronix.api.source2.EventSourceField;
import org.oxymores.chronix.api.source2.EventSourceProvider;
import org.oxymores.chronix.api.source2.OptionAllowsAdditionalFields;
import org.oxymores.chronix.api.source2.RunModeTriggered;
import org.oxymores.chronix.core.engine.api.DTOApplication;

@Component
public class ShellCommandRegistry implements EventSourceProvider, RunModeTriggered, OptionAllowsAdditionalFields
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
    public DTOEventSource newInstance(String name, String description, DTOApplication app, Object... parameters)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        // TODO Auto-generated method stub
        return false;
    }

}
