package org.oxymores.chronix.core.transactional;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class PipelineJob extends TranscientBase {

	private static final long serialVersionUID = -3301527645931127170L;

	@Column(columnDefinition="CHAR(10)")
	String status;
	@Column(columnDefinition="CHAR(36)")
	String runThis;
	Date warnNotEndedAt, mustLaunchBefore, killAt, enteredPipeAt,
			markedForRunAt, beganRunningAt, stoppedRunningAt;
	@Column(columnDefinition="CHAR(36)")
	String level0Id, level1Id, level2Id, level3Id; // Actually UUID

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

	
	public UUID getLevel0IdU() {
		return UUID.fromString(level0Id);
	}

	public void setLevel0IdU(UUID level0Id) {
		this.level0Id = level0Id.toString();
	}

	public UUID getLevel1IdU() {
		return UUID.fromString(level1Id);
	}

	public void setLevel1IdU(UUID level1Id) {
		this.level1Id = level1Id.toString();
	}

	public UUID getLevel2IdU() {
		return UUID.fromString(level2Id);
	}

	public void setLevel2IdU(UUID level2Id) {
		this.level2Id = level2Id.toString();
	}

	public UUID getLevel3IdU() {
		return UUID.fromString(level3Id);
	}

	public void setLevel3IdU(UUID level3Id) {
		this.level3Id = level3Id.toString();
	}
	
	protected String getLevel0Id() {
		return level0Id;
	}

	protected void setLevel0Id(String level0Id) {
		this.level0Id = level0Id;
	}

	protected String getLevel1Id() {
		return level1Id;
	}

	protected void setLevel1Id(String level1Id) {
		this.level1Id = level1Id;
	}

	protected String getLevel2Id() {
		return level2Id;
	}

	protected void setLevel2Id(String level2Id) {
		this.level2Id = level2Id;
	}

	protected String getLevel3Id() {
		return level3Id;
	}

	protected void setLevel3Id(String level3Id) {
		this.level3Id = level3Id;
	}
}
