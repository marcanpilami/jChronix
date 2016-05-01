package org.oxymores.chronix.agent.command.api;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API object describing to a plugin what it should run. Given by the agent to the actual run method.
 */
public class CommandDescription implements Serializable
{
    private static final long serialVersionUID = 4046958802809857415L;

    private Map<String, String> pluginParameters = new LinkedHashMap<>();
    private List<Map.Entry<String, String>> parameters = new ArrayList<>();
    private Map<String, String> environmentVariables = new HashMap<>();

    private String runnerCapability;

    private UUID launchId;

    // Log stuff
    private String logFilePath;
    private boolean storeLogFile = true;

    public void addParameter(String name, String value)
    {
        this.parameters.add(new AbstractMap.SimpleImmutableEntry<String, String>(name, value));
    }

    public void addEnvVar(String name, String value)
    {
        this.environmentVariables.put(name, value);
    }

    public void addPluginParameter(String key, String value)
    {
        this.pluginParameters.put(key, value);
    }

    public Map<String, String> getPluginParameters()
    {
        return this.pluginParameters;
    }

    public String getLogFilePath()
    {
        return logFilePath;
    }

    public void setLogFilePath(String logFilePath)
    {
        this.logFilePath = logFilePath;
    }

    public boolean isStoreLogFile()
    {
        return storeLogFile;
    }

    public void setStoreLogFile(boolean storeLogFile)
    {
        this.storeLogFile = storeLogFile;
    }

    public String getRunnerCapability()
    {
        return runnerCapability;
    }

    public void setRunnerCapability(String runnerCapability)
    {
        this.runnerCapability = runnerCapability;
    }

    public List<Map.Entry<String, String>> getParameters()
    {
        return parameters;
    }

    public Map<String, String> getEnvironmentVariables()
    {
        return environmentVariables;
    }

    public UUID getLaunchId()
    {
        return launchId;
    }

    public void setLaunchId(UUID launchId)
    {
        this.launchId = launchId;
    }
}
