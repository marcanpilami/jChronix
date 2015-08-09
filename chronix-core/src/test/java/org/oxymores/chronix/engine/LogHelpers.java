/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership. This file is licensed to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;

class LogHelpers
{
    private static final Logger log = Logger.getLogger(LogHelpers.class);

    public static List<RunLog> displayAllHistory(ChronixContext ctx)
    {
        EntityManager em = ctx.getHistoryEM();
        TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.beganRunningAt", RunLog.class);
        List<RunLog> res = q.getResultList();

        log.info(RunLog.getTitle());
        for (RunLog l : res)
        {
            log.info(l.getLine());
        }

        em.close();
        return res;
    }

    public static List<RunLog> getAllHistory(ChronixContext ctx)
    {
        EntityManager em = ctx.getHistoryEM();
        TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.beganRunningAt", RunLog.class);
        List<RunLog> res = q.getResultList();
        em.close();
        return res;
    }

    public static List<RunLog> waitForHistoryCount(ChronixContext ctx, int expected)
    {
        return waitForHistoryCount(ctx, expected, 60);
    }

    public static List<RunLog> waitForHistoryCount(ChronixContext ctx, int expected, int timeoutSec)
    {
        List<RunLog> res = null;
        long s = (new DateTime()).getMillis();
        int nb = 0;
        while (nb < expected && (new DateTime()).getMillis() - s < timeoutSec * 1000)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            EntityManager em = ctx.getHistoryEM();
            TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r WHERE r.lastKnownStatus = 'DONE' ORDER BY r.beganRunningAt", RunLog.class);
            res = q.getResultList();
            em.close();
            nb = res.size();
        }
        return res;
    }
}
