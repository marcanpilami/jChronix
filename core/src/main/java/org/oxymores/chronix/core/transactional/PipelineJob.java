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
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.RunDescription;
import org.oxymores.chronix.engine.RunResult;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class PipelineJob extends TranscientBase
{
	private static final long serialVersionUID = -3301527645931127170L;
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(PipelineJob.class);

	@Column(columnDefinition = "CHAR(20)", length = 20)
	String status;
	@Column(length = 255)
	String runThis;
	Date warnNotEndedAt, mustLaunchBefore, killAt, enteredPipeAt, markedForRunAt, beganRunningAt, stoppedRunningAt;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	String level0Id, level1Id, level2Id, level3Id; // Actually UUID

	HashMap<Integer, String> paramValues;

	Boolean outOfPlan = false;
	Integer resultCode = -1;

	public PipelineJob()
	{
		super();
		paramValues = new HashMap<Integer, String>();
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
		paramValues.put(index, value);
	}

	public String getParamValue(int index)
	{
		try
		{
			return paramValues.get(index);
		} catch (Exception e)
		{
			return null;
		}
	}

	protected HashMap<Integer, String> getParamValues()
	{
		return paramValues;
	}

	protected void setParamValues(HashMap<Integer, String> paramValues)
	{
		this.paramValues = paramValues;
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
			this.runThis = null;
		else
			this.runThis = runThis.substring(0, Math.min(255, runThis.length()));
	}

	public Date getWarnNotEndedAt()
	{
		return warnNotEndedAt;
	}

	public void setWarnNotEndedAt(Date warnNotEndedAt)
	{
		this.warnNotEndedAt = warnNotEndedAt;
	}

	public Date getMustLaunchBefore()
	{
		return mustLaunchBefore;
	}

	public void setMustLaunchBefore(Date mustLaunchBefore)
	{
		this.mustLaunchBefore = mustLaunchBefore;
	}

	public Date getKillAt()
	{
		return killAt;
	}

	public void setKillAt(Date killAt)
	{
		this.killAt = killAt;
	}

	public Date getEnteredPipeAt()
	{
		return enteredPipeAt;
	}

	public void setEnteredPipeAt(DateTime enteredPipeAt)
	{
		this.setEnteredPipeAt(enteredPipeAt.toDate());
	}

	public void setEnteredPipeAt(Date enteredPipeAt)
	{
		this.enteredPipeAt = enteredPipeAt;
	}

	public Date getMarkedForRunAt()
	{
		return markedForRunAt;
	}

	public void setMarkedForRunAt(DateTime markedForRunAt)
	{
		this.setMarkedForRunAt(markedForRunAt.toDate());
	}

	public void setMarkedForRunAt(Date markedForRunAt)
	{
		this.markedForRunAt = markedForRunAt;
	}

	public Date getBeganRunningAt()
	{
		return beganRunningAt;
	}

	public void setBeganRunningAt(DateTime beganRunningAt)
	{
		this.setBeganRunningAt(beganRunningAt.toDate());
	}

	public void setBeganRunningAt(Date beganRunningAt)
	{
		this.beganRunningAt = beganRunningAt;
	}

	public Date getStoppedRunningAt()
	{
		return stoppedRunningAt;
	}

	public void setStoppedRunningAt(Date stoppedRunningAt)
	{
		this.stoppedRunningAt = stoppedRunningAt;
	}

	public UUID getLevel0IdU()
	{
		if (this.level0Id == null)
			return null;
		return UUID.fromString(level0Id);
	}

	public void setLevel0IdU(UUID level0Id)
	{
		if (level0Id == null)
			this.level0Id = null;
		else
			this.level0Id = level0Id.toString();
	}

	public UUID getLevel1IdU()
	{
		if (this.level1Id == null)
			return null;
		return UUID.fromString(level1Id);
	}

	public void setLevel1IdU(UUID level1Id)
	{
		if (level1Id == null)
			this.level1Id = null;
		else
			this.level1Id = level1Id.toString();
	}

	public UUID getLevel2IdU()
	{
		if (this.level2Id == null)
			return null;
		return UUID.fromString(level2Id);
	}

	public void setLevel2IdU(UUID level2Id)
	{
		if (level2Id == null)
			this.level2Id = null;
		else
			this.level2Id = level2Id.toString();
	}

	public UUID getLevel3IdU()
	{
		if (this.level3Id == null)
			return null;
		return UUID.fromString(level3Id);
	}

	public void setLevel3IdU(UUID level3Id)
	{
		if (level3Id == null)
			this.level3Id = null;
		else
			this.level3Id = level3Id.toString();
	}

	protected String getLevel0Id()
	{
		return level0Id;
	}

	protected void setLevel0Id(String level0Id)
	{
		this.level0Id = level0Id;
	}

	public String getLevel1Id()
	{
		return level1Id;
	}

	protected void setLevel1Id(String level1Id)
	{
		this.level1Id = level1Id;
	}

	public String getLevel2Id()
	{
		return level2Id;
	}

	protected void setLevel2Id(String level2Id)
	{
		this.level2Id = level2Id;
	}

	public String getLevel3Id()
	{
		return level3Id;
	}

	protected void setLevel3Id(String level3Id)
	{
		this.level3Id = level3Id;
	}

	public boolean isReady(ChronixContext ctx)
	{
		ActiveNodeBase a = this.getActive(ctx);
		return a.getParameters().size() == this.paramValues.size();
	}

	//
	// //////////////////////////////////////////////////////////////////////

	public RunDescription getRD(ChronixContext ctx)
	{
		RunDescription rd = new RunDescription();

		// Command to run
		rd.command = this.runThis;

		// Misc.
		rd.outOfPlan = this.outOfPlan;
		rd.placeName = this.getPlace(ctx).getName();
		rd.activeSourceName = this.getActive(ctx).getName();

		// The IDs that will allow to find the PJ at the end
		rd.id1 = this.getId();
		rd.id2 = this.getActive(ctx).getId();

		// All resolved parameters should be described
		ArrayList<Parameter> prms = this.getActive(ctx).getParameters();
		for (int i = 0; i < prms.size(); i++)
		{
			rd.paramNames.add(prms.get(i).getKey());
			rd.paramValues.add(this.paramValues.get(i));
		}

		// All environment variables should be included
		for (EnvironmentValue ev : this.envParams)
		{
			rd.envNames.add(ev.getKey());
			rd.envValues.add(ev.getValue());
		}

		// Execution method is determined by the source
		rd.Method = this.getActive(ctx).getActivityMethod();

		// Run description is complete, on to the actual execution!
		return rd;
	}

	public Event createEvent()
	{
		RunResult rr = new RunResult();
		rr.returnCode = this.resultCode;
		return createEvent(rr);
	}

	public Event createEvent(RunResult rr)
	{
		Event e = new Event();
		e.localOnly = false;
		e.analysed = false;
		e.conditionData1 = rr.returnCode;
		e.conditionData2 = rr.conditionData2;
		e.conditionData3 = rr.conditionData3;
		e.conditionData4 = rr.conditionData4;
		e.level0Id = this.level0Id;
		e.level1Id = this.level1Id;
		e.level2Id = this.level2Id;
		e.level3Id = this.level3Id;
		e.placeID = this.placeID;
		e.stateID = this.stateID;
		e.appID = this.appID;
		e.activeID = this.activeID;
		e.createdAt = new Date();

		// Report environment
		for (EnvironmentValue ev : this.envParams)
		{
			e.envParams.add(new EnvironmentValue(ev.getKey(), ev.getValue()));
		}
		for (String name : rr.newEnvVars.keySet())
		{
			e.envParams.add(new EnvironmentValue(name, rr.newEnvVars.get(name)));
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
		Application a = ctx.applicationsById.get(UUID.fromString(this.appID));
		Place p = a.getPlace(UUID.fromString(this.placeID));
		ActiveNodeBase act = this.getActive(ctx);

		rlog.activeNodeId = this.activeID;
		rlog.activeNodeName = act.getName();
		rlog.applicationId = this.appID;
		rlog.applicationName = a.getName();
		rlog.beganRunningAt = this.beganRunningAt;
		rlog.chainLaunchId = this.level1Id;
		rlog.chainId = this.level0Id;
		// rlog.chainLev1Id = this.level1Id;
		// rlog.chainLev1Name
		rlog.chainName = a.getActiveNode(UUID.fromString(this.level0Id)).getName();
		// rlog.dataIn =
		// rlog.dataOut =
		rlog.dns = rr.envtServer;
		rlog.enteredPipeAt = this.enteredPipeAt;
		rlog.executionNodeId = p.getNode().getId().toString();
		rlog.executionNodeName = p.getNode().getBrokerUrl();
		rlog.id = this.id.toString();
		rlog.lastKnownStatus = this.status;
		rlog.markedForUnAt = this.markedForRunAt;
		rlog.osAccount = rr.envtUser;
		rlog.placeId = this.placeID;
		rlog.placeName = p.getName();
		rlog.resultCode = this.resultCode;
		// rlog.sequence =
		rlog.shortLog = rr.logStart;
		rlog.stateId = this.stateID;
		rlog.stoppedRunningAt = this.stoppedRunningAt;
		rlog.visible = act.visibleInHistory();
		rlog.whatWasRun = this.runThis;
		rlog.logPath = rr.logPath;

		// Calendar
		if (this.calendarID != null)
		{
			Calendar c = a.getCalendar(UUID.fromString(this.calendarID));
			rlog.calendarName = c.getName();
			rlog.calendarOccurrence = c.getDay(UUID.fromString(this.calendarOccurrenceID)).getValue();
		}

		return rlog;
	}
}
