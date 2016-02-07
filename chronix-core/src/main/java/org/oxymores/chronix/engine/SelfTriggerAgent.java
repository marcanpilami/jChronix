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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.oxymores.chronix.core.EventSourceContainer;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SelfTriggerAgent extends Thread
{
    private static final Logger log = LoggerFactory.getLogger(SelfTriggerAgent.class);

    protected Semaphore loop;
    protected boolean run = true;
    protected List<EventSourceContainer> nodes;
    protected ChronixContextMeta ctxMeta;
    protected ChronixContextTransient ctxDb;
    protected ExecutionNode localNode;
    protected MessageProducer producerEvents;
    protected Session jmsSession;
    protected Semaphore triggering;
    protected DateTime nextLoopVirtualTime;

    void stopAgent()
    {
        try
        {
            this.triggering.acquire();
        }
        catch (InterruptedException e1)
        {
            // Not an issue.
        }
        try
        {
            this.producerEvents.close();
            this.jmsSession.close();
        }
        catch (JMSException e)
        {
            log.warn("An error occurred during SelfTriggerAgent shutdown. Not a problem, but please report it.", e);
        }
        run = false;
        loop.release();
    }

    public void startAgent(ChronixEngine engine) throws JMSException
    {
        this.startAgent(engine, DateTime.now());
    }

    private void startAgent(ChronixEngine engine, DateTime startTime) throws JMSException
    {
        log.debug(String.format("Agent responsible for clocks and other active sources will start"));

        // Save pointers
        this.loop = new Semaphore(0);
        this.ctxMeta = engine.getContextMeta();
        this.ctxDb = engine.getContextTransient();
        this.localNode = engine.getLocalNode();
        this.jmsSession = engine.getBroker().getConnection().createSession(true, Session.SESSION_TRANSACTED);
        this.producerEvents = jmsSession.createProducer(null);
        this.triggering = new Semaphore(1);
        this.nextLoopVirtualTime = startTime;

        // Get all self triggered nodes
        this.nodes = new ArrayList<>();
        /*
         * for (Application2 a : this.ctx.getApplications()) { for (ActiveNodeBase n : a.getActiveElements().values()) { if
         * (n.selfTriggered()) { this.nodes.add(n); } } // TODO: select only clocks with local consequences }
         */
        log.debug(String.format("Agent responsible for clocks will handle %s clock nodes", this.nodes.size()));
        for (EventSourceContainer node : this.nodes)
        {
            log.debug(String.format("\t\t" + node.getName()));
        }

        // Start thread
        this.start();
    }

    @Override
    public void run()
    {
        long msToWait = 0;

        while (run)
        {
            // Wait for the required number of seconds
            try
            {
                if (loop.tryAcquire(msToWait, TimeUnit.MILLISECONDS))
                {
                    // Test is only important because ignoring a result is a Sonar alert...
                }
            }
            catch (InterruptedException e)
            {
                return;
            }
            if (!run)
            {
                // Don't bother doing the final loop
                break;
            }

            try
            {
                this.triggering.acquire();
            }
            catch (InterruptedException e1)
            {
                // Not an issue
            }

            // Log
            log.debug("Self trigger agent loops");
            this.preLoopHook();
            if (!run)
            {
                break;
            }

            // Init the next loop time at a huge value
            DateTime now = DateTime.now();
            DateTime loopVirtualTime = this.nextLoopVirtualTime;
            this.nextLoopVirtualTime = now.plusDays(1).minusMillis(now.getMillisOfDay());

            // Check if the engine logical time ("present") is consistent with the world's real time ("now") (for warnings only)
            if ((now.compareTo(loopVirtualTime) >= 0 && (new Interval(loopVirtualTime, now)).toDurationMillis() > 1000)
                    || (now.compareTo(loopVirtualTime) < 0 && (new Interval(now, loopVirtualTime)).toDurationMillis() > 1000))
            {
                log.warn("There is more than one second between internal time and system clock time - performance issue?");
            }

            // Loop through all the self triggered nodes and get their next loop virtual time
            try (org.sql2o.Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
            {
                DateTime tmp;
                for (EventSourceContainer n : this.nodes)
                {
                    try
                    {
                        // TODO
                        /* tmp = n.selfTrigger(producerEvents, jmsSession, ctxMeta, conn, loopVirtualTime);
                        if (tmp.compareTo(this.nextLoopVirtualTime) < 0)
                        {
                            this.nextLoopVirtualTime = tmp;
                        }*/
                    }
                    catch (Exception e)
                    {
                        log.error("Error triggering clocks and the like", e);
                    }
                }

                // Commit
                try
                {
                    jmsSession.commit();
                }
                catch (JMSException e)
                {
                    log.error("Oups", e);
                    return;
                }
                conn.commit();
            }
            this.triggering.release();

            msToWait = getNextLoopTime();
            log.debug(String.format("Self trigger agent will loop again in %s milliseconds", msToWait));
        }
    }

    // Default implementation uses system clock.
    protected long getNextLoopTime()
    {
        if (DateTime.now().compareTo(this.nextLoopVirtualTime) < 0)
        {
            Interval i = new Interval(DateTime.now(), this.nextLoopVirtualTime);
            return i.toDurationMillis();
        }
        else
        {
            return 0;
        }
    }

    protected void preLoopHook()
    {}
}
