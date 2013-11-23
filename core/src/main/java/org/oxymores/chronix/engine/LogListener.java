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

import java.util.Date;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.timedata.RunStats;

class LogListener extends BaseListener
{
    private static Logger log = Logger.getLogger(LogListener.class);

    void startListening(Broker b) throws JMSException
    {
        // Base initialization
        this.init(b, true, true);
        log.debug(String.format("(%s) Initializing LogListener", ctx.configurationDirectory));

        // Register current object as a listener on LOG queue
        qName = String.format("Q.%s.LOG", brokerName);
        this.subscribeTo(qName);
    }

    @Override
    public void onMessage(Message msg)
    {
        ObjectMessage omsg = (ObjectMessage) msg;
        RunLog rlog;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof RunLog))
            {
                log.warn("An object was received on the log queue but was not a log! Ignored.");
                jmsCommit();
                return;
            }
            rlog = (RunLog) o;
        }
        catch (JMSException e)
        {
            log.error("An error occurred during log reception. Message will stay in queue and will be re-analysed later", e);
            jmsRollback();
            return;
        }
        trHistory.begin();
        trTransac.begin();

        log.info(String.format("An internal log was received. Id: %s - Target: %s - Place: %s - State: %s", rlog.id, rlog.activeNodeName,
                rlog.placeName, rlog.stateId));
        log.debug("\n" + RunLog.getTitle() + "\n" + rlog.getLine());
        rlog.lastLocallyModified = new Date();
        emHistory.merge(rlog);
        RunStats.storeMetrics(rlog, emTransac);
        trHistory.commit();
        trTransac.commit();
        jmsCommit();

        if (!ctx.simulateExternalPayloads)
        {
            trTransac.begin();
            RunStats.updateStats(rlog, emTransac);
            trTransac.commit();
        }
    }
}
