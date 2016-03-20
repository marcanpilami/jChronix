package org.oxymores.chronix.source.command.dto;

import java.util.HashMap;
import java.util.Map;

import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.source.command.prv.ShellCommandRegistry;

/**
 * The persistence and DTO object describing a command to be run through a given plugin.
 */
public class ShellCommand extends EventSourceTriggered
{
    private static final long serialVersionUID = 8506663160948904408L;

    private String runnerCapability;
    private Map<String, String> pluginParameters = new HashMap<>();

    ///////////////////////////////////////////////////////////////////////////
    // HELPER CONSTRUCTORS
    ///////////////////////////////////////////////////////////////////////////

    ShellCommand()
    {}

    public ShellCommand(String name, String description, String commandToRun, String runnerCapability)
    {
        this.name = name;
        this.runnerCapability = runnerCapability;
        this.pluginParameters.put("COMMAND", commandToRun);
    }

    ///////////////////////////////////////////////////////////////////////////
    // ENGINE METHODS
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        // Just send the command to the command agent
        CommandDescription cd = new CommandDescription();
        cd.setLaunchId(jd.getLaunchId());
        cd.setRunnerCapability(runnerCapability);
        cd.setStoreLogFile(true);
        for (Map.Entry<String, String> prm : cd.getParameters().entrySet())
        {
            cd.addParameter(prm.getKey(), prm.getValue());
        }
        for (Map.Entry<String, String> prm : pluginParameters.entrySet())
        {
            cd.addPluginParameter(prm.getKey(), prm.getValue());
        }
        cb.sendMessage(cd, "Q." + cb.getNodeName() + ".RUNNER", cb.getResultQueueName());

        // This plugin is always asynchronous.
        return null;
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ShellCommandRegistry.class;
    }

    ///////////////////////////////////////////////////////////////////////////
    // STUPID ACCESSORS
    ///////////////////////////////////////////////////////////////////////////

    public String getRunnerCapability()
    {
        return runnerCapability;
    }

    public void setRunnerCapability(String runnerCapability)
    {
        this.runnerCapability = runnerCapability;
    }

    public Map<String, String> getPluginParameters()
    {
        return pluginParameters;
    }

    public void setPluginParameters(Map<String, String> pluginParameters)
    {
        this.pluginParameters = pluginParameters;
    }
}
