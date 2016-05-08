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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.timedata.RunStats;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * Responsible for storing RunLog in the database (i.e. history elements, including a short text log). Sending a copy to the console if
 * necessary is handled at the source (cf. <code>SenderHelpers</code>).
 *
 */
class LogListener implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(LogListener.class);

    private ChronixContextTransient ctxDb;

    LogListener(ChronixContextTransient ctxDb)
    {
        this.ctxDb = ctxDb;
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        ObjectMessage omsg = (ObjectMessage) msg;
        RunLog rlog;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof RunLog))
            {
                log.warn("An object was received on the log queue but was not a log! Ignored.");
                return;
            }
            rlog = (RunLog) o;
        }
        catch (JMSException e)
        {
            throw new ListenerRollbackException(
                    "An error occurred during log reception. Message will stay in queue and will be re-analysed later", e);
        }

        try (Connection connHistory = this.ctxDb.getHistoryDataSource().beginTransaction();
                Connection connTransac = this.ctxDb.getTransacDataSource().beginTransaction();)
        {

            log.info(String.format("An internal log was received. Id: %s - Target: %s - Place: %s - State: %s. RC is: %s. First line: %s",
                    rlog.getId(), rlog.getActiveNodeName(), rlog.getPlaceName(), rlog.getStateId(), rlog.getResultCode(),
                    rlog.getShortLog()));
            log.debug("\n" + RunLog.getTitle() + "\n" + rlog.getLine());
            rlog.setLastLocallyModified(DateTime.now());
            rlog.insertOrUpdate(connHistory);

            RunStats.storeMetrics(rlog, connTransac);
            connHistory.commit();
            connTransac.commit();
        }

        // TODO: remove simulated results
        if (rlog.getStoppedRunningAt() != null && rlog.getResultCode() == 0)
        {
            try (Connection connTransac = this.ctxDb.getTransacDataSource().beginTransaction())
            {
                RunStats.updateStats(rlog, connTransac);
                connTransac.commit();
            }
        }
    }
}
