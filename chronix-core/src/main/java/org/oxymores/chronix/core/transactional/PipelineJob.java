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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.oxymores.chronix.api.prm.AsyncParameterResult;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.ParameterDef;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.timedata.RunStats;
import org.oxymores.chronix.engine.data.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class PipelineJob extends TranscientBase implements JobDescription
{
    private static final Logger log = LoggerFactory.getLogger(PipelineJob.class);
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

    // Format is : parameter UUID, parameter key, parameter value. Order is preserved (LinkedHashMap).
    private transient Map<UUID, String> resolvedParameters;
    private transient Map<String, String> resolvedFields;
    private transient EventSourceDef source;

    public PipelineJob()
    {
        super();
    }

    /////////////////////////////////////////////////////////////////
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

    ////////////////////////
    // Params & fields

    public void setParamOrFieldValue(AsyncParameterResult res, ParameterResolutionRequest rq)
    {
        log.trace("Pipelinejob has received a parameter - prm id is " + rq.getParameterId() + " - key is " + rq.getParameter().getKey()
                + " - value " + res.result);
        ParameterDef targetPrm = this.source.getField(rq.getParameter().getId());
        if (targetPrm != null)
        {
            this.resolvedFields.put(rq.getParameter().getKey(), res.result);
            return;
        }

        targetPrm = this.source.getAdditionalParameter(rq.getParameter().getId());
        if (targetPrm != null)
        {
            this.resolvedParameters.put(rq.getParameter().getId(), res.result);
            return;
        }

        throw new IllegalArgumentException("this pipeline job is not waiting for a field value with key " + rq.getParameter().getKey());
    }

    // Note: taking toRun (and not resolving this.source) because only called from RunnerManager which has already resolved it.
    public void initParamResolution(EventSourceDef toRun)
    {
        this.source = toRun;
        resolvedParameters = new LinkedHashMap<>();
        for (ParameterDef ph : toRun.getAdditionalParameters())
        {
            resolvedParameters.put(ph.getParameterId(), (String) null);
        }
        resolvedFields = new HashMap<>();
    }

    @Override
    public Map<String, String> getFields()
    {
        return new HashMap<>(this.resolvedFields);
    }

    private int resolvedAdditionalPrm()
    {
        int res = 0;
        for (String s : this.resolvedParameters.values())
        {
            if (s != null)
            {
                res++;
            }
        }
        return res;
    }

    /////////////////////
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

    public boolean isReady(ChronixContextMeta ctx)
    {
        // TODO: check if fields are really resolved?
        EventSourceDef a = this.getActive(ctx);
        return a.getAdditionalParameters().size() == resolvedAdditionalPrm() && a.getFields().size() == this.resolvedFields.size();
    }

    //
    ////////////////////////////////////////////////////////////////////////

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

    public RunLog getEventLog(ChronixContextMeta ctx)
    {
        return this.getEventLog(ctx, new RunResult());
    }

    public RunLog getEventLog(ChronixContextMeta ctx, RunResult rr)
    {
        RunLog rlog = new RunLog();
        Application a = ctx.getApplication(this.appID);
        Place p = ctx.getEnvironment().getPlace(this.placeID);
        EventSourceDef act = this.getActive(ctx);

        rlog.setActiveNodeId(this.activeID);
        rlog.setActiveNodeName(act.getName());
        rlog.setApplicationId(this.appID);
        rlog.setApplicationName(a.getName());
        rlog.setBeganRunningAt(this.beganRunningAt);
        rlog.setChainLaunchId(this.level1Id);
        rlog.setChainId(this.level0Id);
        rlog.setChainName(a.getEventSource(this.level0Id).getName());
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
        rlog.setVisible(!act.isHiddenFromHistory());
        rlog.setWhatWasRun(this.runThis);
        rlog.setLogPath(rr.logPath);

        // Calendar
        if (this.calendarID != null)
        {
            FunctionalSequence c = a.getCalendar(this.calendarID);
            rlog.setCalendarName(c.getName());
            rlog.setCalendarOccurrence(c.getOccurrence(this.calendarOccurrenceID).getLabel());
        }

        return rlog;
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

    ///////////////////////////////////////////////////////////////////////////
    // Persistence

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

    ///////////////////////////////////////////////////////////////////////////
    // Public interface

    @Override
    public UUID getEventSourceId()
    {
        return this.activeID;
    }

    @Override
    public UUID getLaunchId()
    {
        return this.id;
    }

    @Override
    public UUID getParentScopeLaunchId()
    {
        return this.level2Id;
    }

    @Override
    public DateTime getVirtualTimeStart()
    {
        return this.virtualTime;
    }

    @Override
    public boolean isOutOfPlan()
    {
        return this.outOfPlan;
    }

    @Override
    public List<Map.Entry<String, String>> getParameters()
    {
        List<Map.Entry<String, String>> res = new ArrayList<>();

        Iterator<ParameterDef> prmDefs = this.source.getAdditionalParameters().iterator();
        for (Map.Entry<UUID, String> e : this.resolvedParameters.entrySet())
        {
            res.add(new AbstractMap.SimpleImmutableEntry<String, String>(prmDefs.next().getKey(), e.getValue()));
        }
        return res;
    }

    @Override
    public Map<String, String> getEnvironment()
    {
        // Note: only the content of the cache. No query inside database done here.
        Map<String, String> res = new HashMap<>();
        for (EnvironmentValue v : this.envValues)
        {
            res.put(v.getKey(), v.getValue());
        }
        return res;
    }
}
