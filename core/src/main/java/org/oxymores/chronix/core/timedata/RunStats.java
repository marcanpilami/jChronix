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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;

@Entity
public class RunStats implements Serializable
{
    private static final long serialVersionUID = -3318147581838188039L;
    private static Logger log = Logger.getLogger(RunStats.class);

    @Column(columnDefinition = "CHAR(36)", length = 36)
    public String stateId;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    public String placeId;
    public float meanDuration;
    public float maxDuration;
    public float minDuration;

    private static RunStats getRS(EntityManager em, String stateId, String placeId)
    {
        RunStats rs = null;
        TypedQuery<RunStats> q = em.createQuery("SELECT rr FROM RunStats rr where rr.placeId = ?1 AND rr.stateId = ?2", RunStats.class);
        q.setParameter(1, placeId);
        q.setParameter(2, stateId);
        try
        {
            rs = q.getSingleResult();
        }
        catch (NoResultException e)
        {
        }
        return rs;
    }

    public static float getMean(EntityManager em, String stateId, String placeId)
    {
        // Retrieve the statistics object
        RunStats rs = RunStats.getRS(em, stateId, placeId);

        // If it does not exist, return default time - 1 minute
        if (rs == null)
        {
            return 60000;
        }

        // Else, return the true result
        return rs.meanDuration;
    }

    // Must be called inside open transaction
    public static void storeMetrics(RunLog rlog, EntityManager em)
    {
        if (rlog.getStoppedRunningAt() != null && rlog.getResultCode() == 0)
        {
            DateTime s = new DateTime(rlog.getBeganRunningAt());
            DateTime e = new DateTime(rlog.getStoppedRunningAt());
            Interval i = new Interval(s, e);
            RunMetrics rm = new RunMetrics();
            rm.duration = i.getEndMillis() - i.getStartMillis();
            rm.placeId = rlog.getPlaceId();
            rm.startTime = rlog.getBeganRunningAt();
            rm.stateId = rlog.getStateId();

            em.persist(rm);
        }
    }

    // Must be called inside open transaction
    public static void updateStats(RunLog rlog, EntityManager em)
    {
        // Retrieve the statistics object
        RunStats rs = RunStats.getRS(em, rlog.getStateId(), rlog.getPlaceId());

        // If it does not exist, create it
        if (rs == null)
        {
            rs = new RunStats();
            rs.placeId = rlog.getPlaceId();
            rs.stateId = rlog.getStateId();
            em.persist(rs);
        }

        // Update calculations
        Query q2 = em
                .createQuery("SELECT AVG(rm.duration) AS A, MAX(rm.duration) AS B, MIN(rm.duration) AS C FROM RunMetrics rm WHERE rm.placeId = ?1 AND rm.stateId = ?2");
        q2.setParameter(1, rlog.getPlaceId());
        q2.setParameter(2, rlog.getStateId());

        Object[] o = (Object[]) q2.getSingleResult();
        rs.meanDuration = (Long) o[0];
        rs.maxDuration = (Long) o[1];
        rs.minDuration = (Long) o[2];

        log.debug(String.format("New run duration mean is now %s ms", rs.meanDuration));

        // Purge all old entries
        TypedQuery<RunMetrics> q3 = em.createQuery(
                "SELECT rr FROM RunMetrics rr where rr.placeId = ?1 AND rr.stateId = ?2 ORDER BY rr.startTime asc", RunMetrics.class);
        q3.setParameter(1, rlog.getPlaceId());
        q3.setParameter(2, rlog.getStateId());
        List<RunMetrics> res = q3.getResultList();
        if (res.size() > 10)
        {
            for (RunMetrics rm : res.subList(10, res.size()))
                em.remove(rm);
        }
    }
}
