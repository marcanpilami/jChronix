/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.engine.modularity.runner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A property bag containing all the data needed by a Runner plugin to actually run something (shell command, SQL procedure...)
 */
public class RunDescription implements Serializable
{
    private static final long serialVersionUID = -7603747840000703435L;

    private String runPlugin;
    private Map<String, String> pluginParameters = new HashMap<>();

    private List<String> paramNames = new ArrayList<>();
    private List<String> paramValues = new ArrayList<>();
    private List<String> envNames = new ArrayList<>();
    private List<String> envValues = new ArrayList<>();

    private Boolean helperExecRequest = false;
    private String reportToQueueName;

    // Log stuff
    private String logFilePath;
    private boolean storeLogFile = true;
    private boolean returnFullerLog = true;
    private Boolean shouldSendLogFile = false;

    // Helper for log file naming
    private String placeName = "";
    private String activeSourceName = "";
    private UUID appID = null;

    // The following data is only useful for the engine, not the runner.
    // It should be put as is in the run result object.
    // ID1 is PJ ID
    private UUID id1;
    // ID2 is the active element ID
    private UUID id2;
    private Boolean outOfPlan = false;

    public void addParameter(String name, String value)
    {
        this.paramNames.add(name);
        this.paramValues.add(value);
    }

    public void addEnvVar(String name, String value)
    {
        this.envNames.add(name);
        this.envValues.add(value);
    }

    public void addPluginParameter(String key, String value)
    {
        this.pluginParameters.put(key, value);
    }

    public Map<String, String> getPluginParameters()
    {
        return this.pluginParameters;
    }

    public String getReportToQueueName()
    {
        return reportToQueueName;
    }

    public void setReportToQueueName(String reportToQueueName)
    {
        this.reportToQueueName = reportToQueueName;
    }

    public List<String> getParamNames()
    {
        return paramNames;
    }

    public void setParamNames(List<String> paramNames)
    {
        this.paramNames = paramNames;
    }

    public List<String> getParamValues()
    {
        return paramValues;
    }

    public void setParamValues(List<String> paramValues)
    {
        this.paramValues = paramValues;
    }

    public List<String> getEnvNames()
    {
        return envNames;
    }

    public void setEnvNames(List<String> envNames)
    {
        this.envNames = envNames;
    }

    public List<String> getEnvValues()
    {
        return envValues;
    }

    public void setEnvValues(List<String> envValues)
    {
        this.envValues = envValues;
    }

    public Boolean getHelperExecRequest()
    {
        return helperExecRequest;
    }

    public void setHelperExecRequest(Boolean helperExecRequest)
    {
        this.helperExecRequest = helperExecRequest;
    }

    public Boolean getShouldSendLogFile()
    {
        return shouldSendLogFile;
    }

    public void setShouldSendLogFile(Boolean shouldSendLogFile)
    {
        this.shouldSendLogFile = shouldSendLogFile;
    }

    public String getRunPlugin()
    {
        return runPlugin;
    }

    public void setRunPlugin(String plugin)
    {
        this.runPlugin = plugin;
    }

    public String getPlaceName()
    {
        return placeName;
    }

    public void setPlaceName(String placeName)
    {
        this.placeName = placeName;
    }

    public String getActiveSourceName()
    {
        return activeSourceName;
    }

    public void setActiveSourceName(String activeSourceName)
    {
        this.activeSourceName = activeSourceName;
    }

    public UUID getAppID()
    {
        return appID;
    }

    public void setAppID(UUID appID)
    {
        this.appID = appID;
    }

    public UUID getId1()
    {
        return id1;
    }

    public void setId1(UUID id1)
    {
        this.id1 = id1;
    }

    public UUID getId2()
    {
        return id2;
    }

    public void setId2(UUID id2)
    {
        this.id2 = id2;
    }

    public Boolean getOutOfPlan()
    {
        return outOfPlan;
    }

    public void setOutOfPlan(Boolean outOfPlan)
    {
        this.outOfPlan = outOfPlan;
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

    public boolean isReturnFullerLog()
    {
        return returnFullerLog;
    }

    public void setReturnFullerLog(boolean returnFullerLog)
    {
        this.returnFullerLog = returnFullerLog;
    }
}
