package org.oxymores.chronix.core.transactional;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class Event extends TranscientBase {

	private static final long serialVersionUID = 2488490723929455210L;

	protected Date bestBefore;
	protected boolean localOnly, analysed;

	protected Integer conditionData1;
	@Column(columnDefinition="CHAR(50)")
	protected String conditionData2, conditionData3;
	@Column(columnDefinition="CHAR(36)")
	protected String conditionData4; // Well, UUID actually

	@Column(columnDefinition="CHAR(36)")
	protected String level0Id, level1Id; // Same

	public Date getBestBefore() {
		return bestBefore;
	}

	public void setBestBefore(Date bestBefore) {
		this.bestBefore = bestBefore;
	}

	public boolean isLocalOnly() {
		return localOnly;
	}

	public void setLocalOnly(boolean localOnly) {
		this.localOnly = localOnly;
	}

	public boolean isAnalysed() {
		return analysed;
	}

	public void setAnalysed(boolean analysed) {
		this.analysed = analysed;
	}

	public Integer getConditionData1() {
		return conditionData1;
	}

	public void setConditionData1(Integer conditionData1) {
		this.conditionData1 = conditionData1;
	}

	public String getConditionData2() {
		return conditionData2;
	}

	public void setConditionData2(String conditionData2) {
		this.conditionData2 = conditionData2;
	}

	public String getConditionData3() {
		return conditionData3;
	}

	public void setConditionData3(String conditionData3) {
		this.conditionData3 = conditionData3;
	}

	public UUID getConditionData4U() {
		return UUID.fromString(conditionData4);
	}

	public void setConditionData4U(UUID conditionData4) {
		this.conditionData4 = conditionData4.toString();
	}

	protected String getConditionData4() {
		return conditionData4;
	}

	protected void setConditionData4(String conditionData4) {
		this.conditionData4 = conditionData4;
	}

	public UUID getLevel0IdU() {
		return UUID.fromString(level0Id);
	}

	public void setLevel0IdU(UUID level0Id) {
		this.level0Id = level0Id.toString();
	}

	protected String getLevel0Id() {
		return level0Id;
	}

	protected void setLevel0Id(String level0Id) {
		this.level0Id = level0Id;
	}

	public UUID getLevel1IdU() {
		return UUID.fromString(level1Id);
	}

	public void setLevel1IdU(UUID level1Id) {
		this.level1Id = level1Id.toString();
	}

	protected String getLevel1Id() {
		return level1Id;
	}

	protected void setLevel1Id(String level1Id) {
		this.level1Id = level1Id;
	}
}
