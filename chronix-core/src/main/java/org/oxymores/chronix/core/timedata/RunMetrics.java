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
package org.oxymores.chronix.core.timedata;

import java.io.Serializable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.sql2o.Connection;

public class RunMetrics implements Serializable
{
    private static final long serialVersionUID = -4424619647312566179L;

    @NotNull
    private UUID stateId;

    @NotNull
    private UUID placeId;

    private Long duration;

    private DateTime startTime;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    public UUID getStateId()
    {
        return stateId;
    }

    public void setStateId(UUID stateId)
    {
        this.stateId = stateId;
    }

    public UUID getPlaceId()
    {
        return placeId;
    }

    public void setPlaceId(UUID placeId)
    {
        this.placeId = placeId;
    }

    public Long getDuration()
    {
        return duration;
    }

    public void setDuration(Long duration)
    {
        this.duration = duration;
    }

    public DateTime getStartTime()
    {
        return startTime;
    }

    public void setStartTime(DateTime startTime)
    {
        this.startTime = startTime;
    }

    public void insert(Connection conn)
    {
        conn.createQuery("INSERT INTO RunMetrics(duration, placeId, startTime, stateId)"
                + " VALUES(:duration, :placeId, :startTime, :stateId)").bind(this).executeUpdate();
    }
}
