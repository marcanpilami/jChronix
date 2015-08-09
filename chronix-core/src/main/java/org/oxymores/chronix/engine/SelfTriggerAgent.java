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

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;

class SelfTriggerAgent extends Thread
{
    private static final Logger log = Logger.getLogger(SelfTriggerAgent.class);

    protected Semaphore loop;
    protected boolean run = true;
    protected List<ActiveNodeBase> nodes;
    protected EntityManager em;
    protected ChronixContext ctx;
    protected MessageProducer producerEvents;
    protected Session jmsSession;
    protected EntityTransaction jpaTransaction;
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

    public void startAgent(ChronixContext ctx, Connection cnx) throws JMSException
    {
        this.startAgent(ctx, cnx, DateTime.now());
    }

    void startAgent(ChronixContext ctx, Connection cnx, DateTime startTime) throws JMSException
    {
        log.debug(String.format("Agent responsible for clocks will start"));

        // Save pointers
        this.loop = new Semaphore(0);
        this.em = ctx.getTransacEM();
        this.ctx = ctx;
        this.jmsSession = cnx.createSession(true, Session.SESSION_TRANSACTED);
        this.producerEvents = jmsSession.createProducer(null);
        this.jpaTransaction = this.em.getTransaction();
        this.triggering = new Semaphore(1);
        this.nextLoopVirtualTime = startTime;

        // Get all self triggered nodes
        this.nodes = new ArrayList<>();
        for (Application a : this.ctx.getApplications())
        {
            for (ActiveNodeBase n : a.getActiveElements().values())
            {
                if (n.selfTriggered())
                {
                    this.nodes.add(n);
                }
            }
            // TODO: select only clocks with local consequences
        }
        log.debug(String.format("Agent responsible for clocks will handle %s clock nodes", this.nodes.size()));
        for (ActiveNodeBase node : this.nodes)
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

            // Loop through all the self triggered nodes and get their next loop virtual time
            jpaTransaction.begin();
            DateTime tmp = null;
            for (ActiveNodeBase n : this.nodes)
            {
                try
                {
                    tmp = n.selfTrigger(producerEvents, jmsSession, ctx, em, loopVirtualTime);
                }
                catch (Exception e)
                {
                    log.error("Error triggering clocks and the like", e);
                }
                if (tmp.compareTo(this.nextLoopVirtualTime) < 0)
                {
                    this.nextLoopVirtualTime = tmp;
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
            jpaTransaction.commit();

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
    {
    }
}
