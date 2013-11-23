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

package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;

class LogHelpers
{
    private static Logger log = Logger.getLogger(LogHelpers.class);

    public static List<RunLog> displayAllHistory(ChronixContext ctx)
    {
        EntityManager em = ctx.getHistoryEM();
        TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
        List<RunLog> res = q.getResultList();

        log.info(RunLog.getTitle());
        for (RunLog l : res)
        {
            log.info(l.getLine());
        }

        return res;
    }

    public static void clearAllTranscientElements(ChronixContext ctx)
    {
        try
        {
            // Clean history db
            EntityManager em1 = ctx.getHistoryEM();
            EntityTransaction tr1 = em1.getTransaction();

            tr1.begin();
            em1.createQuery("DELETE FROM RunLog r").executeUpdate();
            em1.createQuery("DELETE FROM UserTrace r").executeUpdate();
            tr1.commit();
            em1.close();

            // Clean OP db
            EntityManager em2 = ctx.getTransacEM();
            EntityTransaction tr2 = em2.getTransaction();

            tr2.begin();
            em2.createQuery("DELETE FROM EnvironmentValue r").executeUpdate();
            em2.createQuery("DELETE FROM ClockTick r").executeUpdate();
            em2.createQuery("DELETE FROM EventConsumption r").executeUpdate();
            em2.createQuery("DELETE FROM TokenReservation r").executeUpdate();

            em2.createQuery("DELETE FROM Event r").executeUpdate();
            em2.createQuery("DELETE FROM PipelineJob r").executeUpdate();
            em2.createQuery("DELETE FROM CalendarPointer r").executeUpdate();
            em2.createQuery("DELETE FROM TranscientBase r").executeUpdate();

            em2.createQuery("DELETE FROM RunStats r").executeUpdate();
            em2.createQuery("DELETE FROM RunMetrics r").executeUpdate();
            tr2.commit();
            em2.close();
        }
        catch (Exception e)
        {
            log.warn(e.getMessage(), e);
        }
    }
}
