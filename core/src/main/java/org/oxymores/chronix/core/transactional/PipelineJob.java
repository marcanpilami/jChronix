package org.oxymores.chronix.core.transactional;

import java.util.Date;
import java.util.UUID;

public class PipelineJob extends TranscientBase {

	private static final long serialVersionUID = -3301527645931127170L;

	String status, runThis;
	Date warnNotEndedAt, mustLaunchBefore, killAt, enteredPipeAt,
			markedForRunAt, beganRunningAt, stoppedRunningAt;
	UUID level0Id, level1Id, level2Id, level3Id;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRunThis() {
		return runThis;
	}

	public void setRunThis(String runThis) {
		this.runThis = runThis;
	}

	public Date getWarnNotEndedAt() {
		return warnNotEndedAt;
	}

	public void setWarnNotEndedAt(Date warnNotEndedAt) {
		this.warnNotEndedAt = warnNotEndedAt;
	}

	public Date getMustLaunchBefore() {
		return mustLaunchBefore;
	}

	public void setMustLaunchBefore(Date mustLaunchBefore) {
		this.mustLaunchBefore = mustLaunchBefore;
	}

	public Date getKillAt() {
		return killAt;
	}

	public void setKillAt(Date killAt) {
		this.killAt = killAt;
	}

	public Date getEnteredPipeAt() {
		return enteredPipeAt;
	}

	public void setEnteredPipeAt(Date enteredPipeAt) {
		this.enteredPipeAt = enteredPipeAt;
	}

	public Date getMarkedForRunAt() {
		return markedForRunAt;
	}

	public void setMarkedForRunAt(Date markedForRunAt) {
		this.markedForRunAt = markedForRunAt;
	}

	public Date getBeganRunningAt() {
		return beganRunningAt;
	}

	public void setBeganRunningAt(Date beganRunningAt) {
		this.beganRunningAt = beganRunningAt;
	}

	public Date getStoppedRunningAt() {
		return stoppedRunningAt;
	}

	public void setStoppedRunningAt(Date stoppedRunningAt) {
		this.stoppedRunningAt = stoppedRunningAt;
	}

	public UUID getLevel0Id() {
		return level0Id;
	}

	public void setLevel0Id(UUID level0Id) {
		this.level0Id = level0Id;
	}

	public UUID getLevel1Id() {
		return level1Id;
	}

	public void setLevel1Id(UUID level1Id) {
		this.level1Id = level1Id;
	}

	public UUID getLevel2Id() {
		return level2Id;
	}

	public void setLevel2Id(UUID level2Id) {
		this.level2Id = level2Id;
	}

	public UUID getLevel3Id() {
		return level3Id;
	}

	public void setLevel3Id(UUID level3Id) {
		this.level3Id = level3Id;
	}
}
