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

import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class LogHelpers
{
    private static final Logger log = LoggerFactory.getLogger(LogHelpers.class);

    public static List<RunLog> displayAllHistory(ChronixContext ctx)
    {
        List<RunLog> res = getAllHistory(ctx);
        log.info(RunLog.getTitle());
        for (RunLog l : res)
        {
            log.info(l.getLine());
        }
        return res;
    }

    public static List<RunLog> getAllHistory(ChronixContext ctx)
    {
        try (Connection conn = ctx.getHistoryDataSource().open())
        {
            return conn.createQuery("SELECT * FROM RunLog r ORDER BY r.beganRunningAt").executeAndFetch(RunLog.class);
        }
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

            try (Connection conn = ctx.getHistoryDataSource().open())
            {
                res = conn.createQuery("SELECT * FROM RunLog r WHERE r.lastKnownStatus = 'DONE' ORDER BY r.beganRunningAt, r.stoppedRunningAt DESC").executeAndFetch(RunLog.class);
            }
            nb = res.size();
        }
        return res;
    }

    public static void testEventCount(ChronixContext ctx, int nbTheory)
    {
        try (Connection conn = ctx.getTransacDataSource().open())
        {
            int nbEvents = conn.createQuery("SELECT COUNT(1) FROM Event e").executeScalar(Integer.class);
            Assert.assertEquals(nbTheory, nbEvents);
        }
    }

    public static void testCalendarPointerCount(ChronixContext ctx, int nbTheory)
    {
        try (Connection conn = ctx.getTransacDataSource().open())
        {
            int nbEvents = conn.createQuery("SELECT COUNT(1) FROM CalendarPointer e").executeScalar(Integer.class);
            Assert.assertEquals(nbTheory, nbEvents);
        }
    }
}
