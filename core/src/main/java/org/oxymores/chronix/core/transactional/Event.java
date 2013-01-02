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
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;

@Entity
public class Event extends TranscientBase
{
	private static final long serialVersionUID = 2488490723929455210L;

	protected Date bestBefore;
	protected boolean localOnly, analysed;

	protected Integer conditionData1;
	@Column(columnDefinition = "CHAR(50)", length = 50)
	protected String conditionData2, conditionData3;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String conditionData4; // Well, UUID actually

	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String level0Id, level1Id, level2Id, level3Id; // Also UUID

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	protected ArrayList<EventConsumption> consumptions = new ArrayList<EventConsumption>();

	public Date getBestBefore()
	{
		return bestBefore;
	}

	public void setBestBefore(Date bestBefore)
	{
		this.bestBefore = bestBefore;
	}

	public boolean isLocalOnly()
	{
		return localOnly;
	}

	public void setLocalOnly(boolean localOnly)
	{
		this.localOnly = localOnly;
	}

	public boolean isAnalysed()
	{
		return analysed;
	}

	public void setAnalysed(boolean analysed)
	{
		this.analysed = analysed;
	}

	public Integer getConditionData1()
	{
		return conditionData1;
	}

	public void setConditionData1(Integer conditionData1)
	{
		this.conditionData1 = conditionData1;
	}

	public String getConditionData2()
	{
		return conditionData2;
	}

	public void setConditionData2(String conditionData2)
	{
		this.conditionData2 = conditionData2;
	}

	public String getConditionData3()
	{
		return conditionData3;
	}

	public void setConditionData3(String conditionData3)
	{
		this.conditionData3 = conditionData3;
	}

	public UUID getConditionData4U()
	{
		return UUID.fromString(conditionData4);
	}

	public void setConditionData4U(UUID conditionData4)
	{
		this.conditionData4 = conditionData4.toString();
	}

	protected String getConditionData4()
	{
		return conditionData4;
	}

	protected void setConditionData4(String conditionData4)
	{
		this.conditionData4 = conditionData4;
	}

	public UUID getLevel0IdU()
	{
		if (level0Id == null)
			return null;
		return UUID.fromString(level0Id);
	}

	public void setLevel0IdU(UUID level0Id)
	{
		this.level0Id = level0Id.toString();
	}

	protected String getLevel0Id()
	{
		return level0Id;
	}

	protected void setLevel0Id(String level0Id)
	{
		this.level0Id = level0Id;
	}

	public UUID getLevel1IdU()
	{
		if (level1Id == null)
			return null;
		return UUID.fromString(level1Id);
	}

	public void setLevel1IdU(UUID level1Id)
	{
		this.level1Id = level1Id.toString();
	}

	protected String getLevel1Id()
	{
		return level1Id;
	}

	protected void setLevel1Id(String level1Id)
	{
		this.level1Id = level1Id;
	}

	public UUID getLevel2IdU()
	{
		if (level2Id == null)
			return null;
		return UUID.fromString(level2Id);
	}

	public void setLevel2IdU(UUID level2Id)
	{
		this.level2Id = level2Id.toString();
	}

	protected String getLevel2Id()
	{
		return level2Id;
	}

	protected void setLevel2Id(String level2Id)
	{
		this.level2Id = level2Id;
	}

	public UUID getLevel3IdU()
	{
		if (level3Id == null)
			return null;
		return UUID.fromString(level3Id);
	}

	public void setLevel3IdU(UUID level3Id)
	{
		this.level3Id = level3Id.toString();
	}

	protected String getLevel3Id()
	{
		return level3Id;
	}

	protected void setLevel3Id(String level3Id)
	{
		this.level3Id = level3Id;
	}

	public Boolean wasConsumedOnPlace(Place p, State s)
	{
		for (EventConsumption ec : this.consumptions)
		{
			if (ec.stateID.equals(s.getId().toString()) && ec.placeID.equals(p.getId().toString()))
				return true;
		}
		return false;
	}
}
