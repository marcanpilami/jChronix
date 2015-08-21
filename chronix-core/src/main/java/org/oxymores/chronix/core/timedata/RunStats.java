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

import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class RunStats implements Serializable
{
    private static final long serialVersionUID = -3318147581838188039L;
    private static final Logger log = LoggerFactory.getLogger(RunStats.class);
    private static final int UUID_LENGTH = 36;

    @NotNull
    private Long id;

    @NotNull
    private UUID stateId;

    @NotNull
    private UUID placeId;

    private float meanDuration, maxDuration, minDuration;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Statistics calculation
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    private static RunStats getRS(Connection conn, UUID stateId, UUID placeId)
    {
        // null if does not exist
        return conn.createQuery("SELECT * FROM RunStats rr where rr.placeId = :placeId AND rr.stateId = :stateId").
                addParameter("placeId", placeId).addParameter("stateId", stateId).executeAndFetchFirst(RunStats.class);
    }

    public static float getMean(Connection conn, UUID stateId, UUID placeId)
    {
        // Retrieve the statistics object
        RunStats rs = RunStats.getRS(conn, stateId, placeId);

        // If it does not exist, return default time - 1 minute
        if (rs == null)
        {
            return 60000;
        }

        // Else, return the true result
        return rs.meanDuration;
    }

    // Must be called inside open transaction
    public static void storeMetrics(RunLog rlog, Connection conn)
    {
        if (rlog.getStoppedRunningAt() != null && rlog.getResultCode() == 0)
        {
            DateTime s = new DateTime(rlog.getBeganRunningAt());
            DateTime e = new DateTime(rlog.getStoppedRunningAt());
            Interval i = new Interval(s, e);
            RunMetrics rm = new RunMetrics();
            rm.setDuration(i.getEndMillis() - i.getStartMillis());
            rm.setPlaceId(rlog.getPlaceId());
            rm.setStartTime(rlog.getBeganRunningAt());
            rm.setStateId(rlog.getStateId());

            rm.insert(conn);
        }
    }

    // Must be called inside open transaction
    public static void updateStats(RunLog rlog, Connection conn)
    {
        // Retrieve the statistics object
        RunStats rs = RunStats.getRS(conn, rlog.getStateId(), rlog.getPlaceId());

        // If it does not exist, create it
        if (rs == null)
        {
            rs = new RunStats();
            rs.placeId = rlog.getPlaceId();
            rs.stateId = rlog.getStateId();
            rs.insertOrUpdate(conn);
        }

        // Update calculations
        RunStats tmp = conn.createQuery("SELECT AVG(rm.duration) AS meanDuration, MAX(rm.duration) AS maxDuration, "
                + "MIN(rm.duration) AS minDuration FROM RunMetrics rm "
                + "WHERE rm.placeId = :placeId AND rm.stateId = :stateId").addParameter("placeId", rs.placeId)
                .addParameter("stateId", rs.stateId).executeAndFetchFirst(RunStats.class);

        rs.meanDuration = tmp.meanDuration;
        rs.maxDuration = tmp.maxDuration;
        rs.minDuration = tmp.minDuration;

        log.debug(String.format("New run duration mean is now %s ms", rs.meanDuration));

        // Purge all old entries
        conn.createQuery("DELETE FROM RunMetrics rm1 WHERE rm1.id NOT IN (SELECT rm2.id FROM RunMetrics rm2"
                + " WHERE rm2.stateId=:stateId AND rm2.placeId=:placeId AND ROWNUM() < 10 "
                + "ORDER BY rm2.startTime desc)").
                addParameter("stateId", rs.stateId).addParameter("placeId", rs.placeId).executeUpdate();
    }

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

    public float getMeanDuration()
    {
        return meanDuration;
    }

    public void setMeanDuration(float meanDuration)
    {
        this.meanDuration = meanDuration;
    }

    public float getMaxDuration()
    {
        return maxDuration;
    }

    public void setMaxDuration(float maxDuration)
    {
        this.maxDuration = maxDuration;
    }

    public float getMinDuration()
    {
        return minDuration;
    }

    public void setMinDuration(float minDuration)
    {
        this.minDuration = minDuration;
    }

    public long getId()
    {
        return this.id;
    }

    public void insertOrUpdate(Connection conn)
    {
        int i = conn.createQuery("UPDATE RunStats SET maxDuration=:maxDuration, meanDuration=:meanDuration "
                + "WHERE stateId=:stateId AND placeId=:placeId").bind(this).executeUpdate().getResult();
        if (i == 0)
        {
            conn.createQuery("INSERT INTO RunStats(maxDuration, meanDuration, placeId, stateId) "
                    + "VALUES(:maxDuration, :meanDuration, :placeId, :stateId)").bind(this).executeUpdate();
        }
    }
}
