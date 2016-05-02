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

import java.util.UUID;
import javax.validation.constraints.Size;
import org.joda.time.DateTime;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.network.Place;
import org.sql2o.Connection;

public class Event extends TranscientBase implements DTOEvent
{
    private static final long serialVersionUID = 2488490723929455210L;

    private DateTime bestBefore;

    private boolean localOnly, analysed;

    private Integer conditionData1;

    @Size(min = 1, max = 50)
    private String conditionData2, conditionData3;

    private UUID conditionData4;

    public DateTime getBestBefore()
    {
        return bestBefore;
    }

    public void setBestBefore(DateTime bestBefore)
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

    public UUID getConditionData4()
    {
        return conditionData4;
    }

    protected void setConditionData4(UUID conditionData4)
    {
        this.conditionData4 = conditionData4;
    }

    public Boolean wasConsumedOnPlace(Place p, State s, Connection conn)
    {
        int i = conn.createQuery("SELECT COUNT(1) FROM EVENTCONSUMPTION WHERE eventId=:eventId AND stateId=:stateId AND placeId=:placeId")
                .addParameter("eventId", this.id).addParameter("stateId", s.getId()).addParameter("placeId", p.getId())
                .executeScalar(Integer.class);
        return i > 0;
    }

    public void insertOrUpdate(Connection conn)
    {
        // NOTE: we may want to do it the barbarian way: try insert and if key exception do update...
        long nb = conn.createQuery("SELECT COUNT(1) FROM Event WHERE id=:id").addParameter("id", this.id).executeScalar(Long.class);

        if (nb > 0)
        {
            conn.createQuery("UPDATE Event SET analysed = :analysed, bestBefore=:bestBefore, conditionData1 = :conditionData1, "
                    + "conditionData2 = :conditionData2, conditionData3 = :conditionData3, conditionData4 = :conditionData4, "
                    + "localOnly = :localOnly WHERE id=:id").bind(this).executeUpdate();
        }
        else
        {
            conn.createQuery("INSERT INTO Event(id, analysed, bestBefore, conditionData1, conditionData2, conditionData3,"
                    + "conditionData4, localOnly, activeId, appId, calendarID, calendarOccurrenceID, createdAt,"
                    + "ignoreCalendarUpdating, level0Id, level1Id, level2Id, level3Id, outsideChainLaunch, placeId,"
                    + "simulationID, stateID, virtualTime) "
                    + "VALUES(:id, :analysed, :bestBefore, :conditionData1, :conditionData2, :conditionData3,"
                    + ":conditionData4, :localOnly, :activeID, :appID, :calendarID, :calendarOccurrenceID, :createdAt,"
                    + ":ignoreCalendarUpdating, :level0Id, :level1Id, :level2Id, :level3Id, :outsideChainLaunch, :placeID,"
                    + ":simulationID, :stateID, :virtualTime)").bind(this).executeUpdate();
        }

        updateEnvValues(conn);
    }
}
