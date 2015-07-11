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

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class SelfTriggerAgentSim extends SelfTriggerAgent
{
    private static Logger log = Logger.getLogger(SelfTriggerAgentSim.class);
    protected DateTime beginTime, endTime;

    @Override
    protected long getNextLoopTime()
    {
        // During a simulation, no need to actually wait for the correct time to come, just loop!
        return 0;
    }

    private Long getRunningCount()
    {
        Long running = this.em.createQuery("SELECT COUNT(e) FROM Event e WHERE e.analysed = False", Long.class).getSingleResult()
                + this.em.createQuery("SELECT COUNT(p) FROM PipelineJob p WHERE p.status <> 'DONE'", Long.class).getSingleResult();
        log.debug(String.format("There are %s elements unack in the db", running));

        QueueBrowser events = null;
        QueueBrowser pjs = null;
        Queue devents, dpjs;
        @SuppressWarnings("rawtypes")
        Enumeration enu = null;
        try
        {
            String eventQueueName = String.format(Constants.Q_EVENT, this.ctx.getLocalNode().getBrokerName());
            String pjQueueName = String.format(Constants.Q_PJ, this.ctx.getLocalNode().getBrokerName());
            devents = this.jmsSession.createQueue(eventQueueName);
            dpjs = this.jmsSession.createQueue(pjQueueName);
            events = this.jmsSession.createBrowser(devents);
            pjs = this.jmsSession.createBrowser(dpjs);
            enu = events.getEnumeration();
            while (enu.hasMoreElements())
            {
                enu.nextElement();
                running++;
            }
            enu = pjs.getEnumeration();
            while (enu.hasMoreElements())
            {
                enu.nextElement();
                running++;
            }
            pjs.close();
            events.close();
        }
        catch (JMSException e1)
        {
            // Just log - simulation is not critical.
            log.error("An error occured during simulation", e1);
        }
        log.debug(String.format("There are %s elements unack in the db + mq", running));

        return running;
    }

    @Override
    protected void preLoopHook()
    {
        // The clocks can only advance if there are no running elements. That way, multiple runs will not occur simultaneously.
        Long running = getRunningCount();

        while (running > 0)
        {
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                // We'll just loop again, no panic.
            }

            running = getRunningCount();
        }

        // Here, should trigger external events
        // TODO: external events simulation

        // Exit if we are now out of the simulation window
        if (this.endTime.isBefore(this.nextLoopVirtualTime))
        {
            log.debug("STAS will stop");
            try
            {
                this.producerEvents.close();
                this.jmsSession.close();
            }
            catch (JMSException e)
            {
                // Don't care. We are dying anyway.
            }
            run = false;
            loop.release();
        }
    }

    public DateTime getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(DateTime beginTime)
    {
        this.beginTime = beginTime;
        this.nextLoopVirtualTime = beginTime;
    }

    public DateTime getEndTime()
    {
        return endTime;
    }

    public void setEndTime(DateTime endTime)
    {
        log.debug("End of simulation : " + endTime);
        this.endTime = endTime;
    }
}
