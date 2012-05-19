package org.oxymores.chronix.core.transactional;

import java.util.Date;
import java.util.UUID;

public class Event extends TranscientBase {

	private static final long serialVersionUID = 2488490723929455210L;

	protected Date bestBefore;
	protected boolean localOnly, analysed;

	protected Integer conditionData1;
	protected String conditionData2, conditionData3;
	protected UUID conditionData4;

	protected UUID level0Id, level1Id;

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

	public UUID getConditionData4() {
		return conditionData4;
	}

	public void setConditionData4(UUID conditionData4) {
		this.conditionData4 = conditionData4;
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
}
