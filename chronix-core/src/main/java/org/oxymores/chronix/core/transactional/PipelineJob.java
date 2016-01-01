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
package org.oxymores.chronix.core.transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.timedata.RunStats;
import org.oxymores.chronix.engine.modularity.runner.RunDescription;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.sql2o.Connection;

public class PipelineJob extends TranscientBase
{
    private static final long serialVersionUID = -3301527645931127170L;

    @NotNull
    @Size(min = 1, max = 20)
    String status;

    @NotNull
    @Size(min = 1, max = PATH_LENGTH)
    String runThis;

    DateTime warnNotEndedAt, mustLaunchBefore, killAt, enteredPipeAt, markedForRunAt, beganRunningAt, stoppedRunningAt;

    Boolean outOfPlan = false;

    Integer resultCode = -1;

    private final Map<Integer, String> resolvedParameters = new HashMap<>();

    public PipelineJob()
    {
        super();
    }

    // ///////////////////////////////////////////////////////////////
    // Set/Get
    public Integer getResultCode()
    {
        return resultCode;
    }

    public void setResultCode(Integer resultCode)
    {
        this.resultCode = resultCode;
    }

    public Boolean getOutOfPlan()
    {
        return outOfPlan;
    }

    public void setOutOfPlan(Boolean outOfPlan)
    {
        this.outOfPlan = outOfPlan;
    }

    // /////////////
    // Params
    public void setParamValue(Integer index, String value)
    {
        resolvedParameters.put(index, value);
    }

    public String getParamValue(int index)
    {
        return resolvedParameters.get(index);
    }

    protected Map<Integer, String> getParamValues()
    {
        return resolvedParameters;
    }

    // ///////////////////
    // Misc.
    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getRunThis()
    {
        return runThis;
    }

    public void setRunThis(String runThis)
    {
        if (runThis == null)
        {
            this.runThis = null;
        }
        else
        {
            this.runThis = runThis.substring(0, Math.min(255, runThis.length()));
        }
    }

    public DateTime getWarnNotEndedAt()
    {
        return warnNotEndedAt;
    }

    public void setWarnNotEndedAt(DateTime warnNotEndedAt)
    {
        this.warnNotEndedAt = warnNotEndedAt;
    }

    public DateTime getMustLaunchBefore()
    {
        return mustLaunchBefore;
    }

    public void setMustLaunchBefore(DateTime mustLaunchBefore)
    {
        this.mustLaunchBefore = mustLaunchBefore;
    }

    public DateTime getKillAt()
    {
        return killAt;
    }

    public void setKillAt(DateTime killAt)
    {
        this.killAt = killAt;
    }

    public DateTime getEnteredPipeAt()
    {
        return enteredPipeAt;
    }

    public void setEnteredPipeAt(DateTime enteredPipeAt)
    {
        this.enteredPipeAt = enteredPipeAt;
    }

    public DateTime getMarkedForRunAt()
    {
        return markedForRunAt;
    }

    public void setMarkedForRunAt(DateTime markedForRunAt)
    {
        this.markedForRunAt = markedForRunAt;
    }

    public DateTime getBeganRunningAt()
    {
        return beganRunningAt;
    }

    public void setBeganRunningAt(DateTime beganRunningAt)
    {
        this.beganRunningAt = beganRunningAt;
    }

    public DateTime getStoppedRunningAt()
    {
        return stoppedRunningAt;
    }

    public void setStoppedRunningAt(DateTime stoppedRunningAt)
    {
        this.stoppedRunningAt = stoppedRunningAt;
    }

    public boolean isReady(ChronixContext ctx)
    {
        ActiveNodeBase a = this.getActive(ctx);
        return a.getParameters().size() == this.resolvedParameters.size();
    }

    //
    // //////////////////////////////////////////////////////////////////////
    public RunDescription getRD(ChronixContext ctx)
    {
        RunDescription rd = new RunDescription();

        // Misc.
        rd.setOutOfPlan(this.outOfPlan);
        rd.setPlaceName(this.getPlace(ctx).getName());
        rd.setActiveSourceName(this.getActive(ctx).getName());
        rd.setAppID(this.appID);

        // The IDs that will allow to find the PJ at the end
        rd.setId1(this.getId());
        rd.setId2(this.getActive(ctx).getId());

        // All resolved parameters should be described
        ArrayList<Parameter> prms = this.getActive(ctx).getParameters();
        for (int i = 0; i < prms.size(); i++)
        {
            rd.addParameter(prms.get(i).getKey(), this.resolvedParameters.get(i));
        }

        // All environment variables should be included
        for (EnvironmentValue ev : this.envValues)
        {
            rd.addEnvVar(ev.getKey(), ev.getValue());
        }

        // Execution method is determined by the source
        rd.setPluginSelector(this.getActive(ctx).getPlugin());

        // Actual command to run is determined by the plugin from the parameter map
        for (Map.Entry<String, String> e : this.getActive(ctx).getPluginParameters().entrySet())
        {
            rd.addPluginParameter(e.getKey(), e.getValue());
        }

        // Run description is complete, on to the actual execution!
        return rd;
    }

    public Event createEvent()
    {
        RunResult rr = new RunResult();
        rr.returnCode = this.resultCode;
        return createEvent(rr, this.virtualTime);
    }

    public Event createEvent(RunResult rr, DateTime virtualTime)
    {
        Event e = new Event();
        e.setLocalOnly(false);
        e.setAnalysed(false);
        e.setConditionData1(rr.returnCode);
        e.setConditionData2(rr.conditionData2);
        e.setConditionData3(rr.conditionData3);
        e.setConditionData4(rr.conditionData4);
        e.setLevel0Id(this.level0Id);
        e.setLevel1Id(this.level1Id);
        e.setLevel2Id(this.level2Id);
        e.setLevel3Id(this.level3Id);
        e.setPlaceID(this.placeID);
        e.setStateID(this.stateID);
        e.setAppID(this.appID);
        e.setActiveID(this.activeID);
        e.setCreatedAt(DateTime.now());
        e.setVirtualTime(virtualTime);

        // Report environment
        if (this.envValues != null)
        {
            for (EnvironmentValue ev : this.envValues)
            {
                e.addEnvValueToCache(ev.getKey(), ev.getValue());
            }
        }
        for (String name : rr.newEnvVars.keySet())
        {
            e.addEnvValueToCache(name, rr.newEnvVars.get(name));
        }

        return e;
    }

    public RunLog getEventLog(ChronixContext ctx)
    {
        return this.getEventLog(ctx, new RunResult());
    }

    public RunLog getEventLog(ChronixContext ctx, RunResult rr)
    {
        RunLog rlog = new RunLog();
        Application a = ctx.getApplication(this.appID);
        Place p = ctx.getEnvironment().getPlace(this.placeID);
        ActiveNodeBase act = this.getActive(ctx);

        rlog.setActiveNodeId(this.activeID);
        rlog.setActiveNodeName(act.getName());
        rlog.setApplicationId(this.appID);
        rlog.setApplicationName(a.getName());
        rlog.setBeganRunningAt(this.beganRunningAt);
        rlog.setChainLaunchId(this.level1Id);
        rlog.setChainId(this.level0Id);
        rlog.setChainName(a.getActiveNode(this.level0Id).getName());
        rlog.setDns(rr.envtServer);
        rlog.setEnteredPipeAt(this.enteredPipeAt);
        rlog.setExecutionNodeId(p.getNode().getId());
        rlog.setExecutionNodeName(p.getNode().getName());
        rlog.setId(this.id);
        rlog.setLastKnownStatus(this.status);
        rlog.setMarkedForUnAt(this.markedForRunAt);
        rlog.setOsAccount(rr.envtUser);
        rlog.setPlaceId(this.placeID);
        rlog.setPlaceName(p.getName());
        rlog.setResultCode(this.resultCode);
        rlog.setShortLog(rr.logStart);
        rlog.setStateId(this.stateID);
        rlog.setStoppedRunningAt(this.stoppedRunningAt);
        rlog.setVisible(act.visibleInHistory());
        rlog.setWhatWasRun(this.runThis);
        rlog.setLogPath(rr.logPath);

        // Calendar
        if (this.calendarID != null)
        {
            Calendar c = a.getCalendar(this.calendarID);
            rlog.setCalendarName(c.getName());
            rlog.setCalendarOccurrence(c.getDay(this.calendarOccurrenceID).getValue());
        }

        return rlog;
    }

    public RunResult getDisabledResult()
    {
        RunResult res = new RunResult();
        res.returnCode = 0;
        res.id1 = this.id;
        res.outOfPlan = this.outOfPlan;
        res.logStart = "was not run as it was marked as disabled";

        res.start = this.getVirtualTime();
        res.end = this.getVirtualTime();

        return res;
    }

    public RunResult getSimulatedResult(Connection connTransac)
    {
        RunResult res = new RunResult();
        res.returnCode = 0;
        res.id1 = this.id;
        res.outOfPlan = this.outOfPlan;
        res.logStart = "simulated run";

        res.start = this.getVirtualTime();
        res.end = new DateTime(res.start).plusSeconds((int) RunStats.getMean(connTransac, this.stateID, this.placeID));

        return res;
    }

    public void insertOrUpdate(Connection conn)
    {
        int i = conn
                .createQuery("UPDATE PipelineJob SET beganRunningAt=:beganRunningAt, enteredPipeAt=:enteredPipeAt"
                        + ", killAt=:killAt, markedForRunAt=:markedForRunAt, mustLaunchBefore=:mustLaunchBefore"
                        + ", outOfPlan=:outOfPlan, resultCode=:resultCode, runThis=:runThis, status=:status"
                        + ", stoppedRunningAt=:stoppedRunningAt, warnNotEndedAt=:warnNotEndedAt WHERE id=:id")
                .bind(this).executeUpdate().getResult();
        if (i == 0)
        {
            conn.createQuery("INSERT INTO PipelineJob(id, beganRunningAt, enteredPipeAt, killAt, markedForRunAt, "
                    + "mustLaunchBefore, outOfPlan, resultCode, runThis, status, stoppedRunningAt, "
                    + "warnNotEndedAt, activeId, appId, calendarID, calendarOccurrenceID, createdAt,"
                    + "ignoreCalendarUpdating, level0Id, level1Id, level2Id, level3Id, outsideChainLaunch, placeId,"
                    + "simulationID, stateID, virtualTime) " + "VALUES(:id, :beganRunningAt, :enteredPipeAt, :killAt, :markedForRunAt, "
                    + ":mustLaunchBefore, :outOfPlan, :resultCode, :runThis, :status, :stoppedRunningAt, "
                    + ":warnNotEndedAt, :activeID, :appID, :calendarID, :calendarOccurrenceID, :createdAt,"
                    + ":ignoreCalendarUpdating, :level0Id, :level1Id, :level2Id, :level3Id, :outsideChainLaunch, :placeID,"
                    + ":simulationID, :stateID, :virtualTime)").bind(this).executeUpdate();
        }

        updateEnvValues(conn);
    }
}
