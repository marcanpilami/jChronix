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
package org.oxymores.chronix.core.active;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.Valid;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RRule;

import org.slf4j.Logger;
import org.hibernate.validator.constraints.Range;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.ClockTick;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixRunException;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class Clock extends ActiveNodeBase
{
    private static final long serialVersionUID = -5203055591135192345L;
    private static final Logger log = LoggerFactory.getLogger(Clock.class);
    private static final String LOG_DATE_FORMAT = "dd/MM/YYYY hh:mm:ss";

    org.joda.time.DateTime created;

    // Validity in minutes. Should always be 0 here.
    @Range(min = 0, max = 0)
    int duration = 0;

    // Relationships
    @Valid
    List<ClockRRule> rulesADD, rulesEXC;

    // Helpers for engine methods
    private transient PeriodList occurrenceCache;
    private transient org.joda.time.DateTime nextCacheRefresh;
    private transient PipelineJob pj;

    // /////////////////////////////////////////////////////////////////////
    // Constructor
    public Clock()
    {
        rulesADD = new ArrayList<>();
        rulesEXC = new ArrayList<>();
        created = org.joda.time.DateTime.now();
        created = created.minusMillis(created.getMillisOfSecond());
        created = created.minusSeconds(created.getSecondOfMinute());
    }

    private PipelineJob getHelperPj()
    {
        if (pj == null)
        {
            pj = new PipelineJob();
            pj.setApplication(this.application);
            pj.setBeganRunningAt(org.joda.time.DateTime.now());
            pj.setEnteredPipeAt(org.joda.time.DateTime.now());
            pj.setMarkedForRunAt(org.joda.time.DateTime.now());
            pj.setOutOfPlan(false);
            pj.setOutsideChain(true);
            pj.setResultCode(0);
            pj.setStatus("DONE");
        }
        return pj;
    }

    //
    // /////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////
    // iCal stuff
    private VEvent getEvent() throws ParseException
    {
        VEvent evt = new VEvent();
        evt.getProperties().add(new net.fortuna.ical4j.model.property.Duration(new Dur(0, 0, this.duration, 0)));

        for (ClockRRule r : rulesADD)
        {
            Recur recur = r.getRecur();
            evt.getProperties().add(new RRule(recur));
        }
        for (ClockRRule r : rulesEXC)
        {
            evt.getProperties().add(new ExRule(r.getRecur()));
        }

        return evt;
    }

    public PeriodList getOccurrences(org.joda.time.DateTime start, org.joda.time.DateTime end) throws ParseException
    {
        DateTime from = new DateTime(start.toDate());
        DateTime to = new DateTime(end.toDate());

        log.debug("Computing occurrences from {} to {} (event duration not taken into account).", from, to);
        VEvent evt = this.getEvent();
        evt.getProperties().add(new DtStart(new DateTime(start.minusSeconds(1).toDate())));

        log.debug("Event start time is {} - creation is {}", evt.getStartDate(), evt.getCreated());
        Period p = new Period(from, to);
        return evt.calculateRecurrenceSet(p);
    }

    //
    // /////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////
    // Stupid GET/SET
    public int getDURATION()
    {
        return duration;
    }

    public void setDURATION(int dURATION)
    {
        duration = dURATION;
    }

    public org.joda.time.DateTime getCREATED()
    {
        return created;
    }

    public List<ClockRRule> getRulesADD()
    {
        return rulesADD;
    }

    public List<ClockRRule> getRulesEXC()
    {
        return rulesEXC;
    }

    // stupid GET/SET
    // /////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////
    // Relationships ADD/REMOVE
    public void addRRuleADD(ClockRRule rule)
    {
        if (!rulesADD.contains(rule))
        {
            rulesADD.add(rule);
        }
    }

    public void removeRRuleADD(ClockRRule rule)
    {
        if (rulesADD.contains(rule))
        {
            rulesADD.remove(rule);
        }
    }

    public void addRRuleEXC(ClockRRule rule)
    {
        if (!rulesEXC.contains(rule))
        {
            rulesEXC.add(rule);
        }
    }

    public void removeRRuleEXC(ClockRRule rule)
    {
        if (rulesEXC.contains(rule))
        {
            rulesEXC.remove(rule);
        }
    }

    //
    // /////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////
    // Scheduling engine methods
    @Override
    public boolean visibleInHistory()
    {
        return false;
    }

    @Override
    public boolean selfTriggered()
    {
        return true;
    }

    @Override
    public org.joda.time.DateTime selfTrigger(MessageProducer eventProducer, Session jmsSession, ChronixContext ctx, Connection conn,
            org.joda.time.DateTime virtualTime) throws ChronixRunException
    {
        getHelperPj();
        pj.setVirtualTime(virtualTime);
        org.joda.time.DateTime nowminusgrace = virtualTime.minusMinutes(this.duration);

        if (occurrenceCache == null || nextCacheRefresh == null || virtualTime.isAfter(nextCacheRefresh))
        {
            try
            {
                occurrenceCache = this.getOccurrences(nowminusgrace, virtualTime.plusHours(25));
                nextCacheRefresh = virtualTime.plusHours(24); // One hour margin is tad big.
            }
            catch (ParseException e)
            {
                log.error("Could not parse an iCal string. A clock has failed.", e);
                throw new ChronixRunException("", e);
            }
            log.debug(String.format("%s occurrences were added to cache", occurrenceCache.size()));
        }

        // Select the occurrences that should be active
        ArrayList<org.joda.time.DateTime> theory = new ArrayList<>();
        for (Object p : occurrenceCache)
        {
            org.joda.time.DateTime from = new org.joda.time.DateTime(((Period) p).getStart());
            org.joda.time.DateTime to = new org.joda.time.DateTime(((Period) p).getEnd());

            if (from.compareTo(virtualTime) <= 0 && to.compareTo(virtualTime) >= 0)
            {
                theory.add(from);
                log.trace(from.toString(LOG_DATE_FORMAT) + " - " + to.toString(LOG_DATE_FORMAT));
            }
        }
        log.debug(String.format("There are %s clock ticks that should be active at %s", theory.size(), virtualTime.toString(LOG_DATE_FORMAT)));

        // Select the ones that are active
        List<ClockTick> real = conn.createQuery("SELECT t.* FROM ClockTick t WHERE t.tickTime >= :laterThan ORDER BY t.tickTime")
                .addParameter("laterThan", nowminusgrace.toDate()).executeAndFetch(ClockTick.class);
        log.debug(String.format("There are %s clock ticks that really are active", real.size()));

        // Select the ones that will have to be created
        List<org.joda.time.DateTime> toCreate = theory.subList(Math.min(real.size(), theory.size()), theory.size());
        log.debug(String.format("%s ticks will have to be created", toCreate.size()));

        // //////////////////////////
        // Create events
        // Get all states that use this clock
        List<State> states = this.getClientStates();

        // Create events through the helper PJ
        for (org.joda.time.DateTime dt : toCreate)
        {
            pj.setVirtualTime(virtualTime);
            for (State s : states)
            {
                pj.setState(s);
                pj.setLevel1Id(new UUID(0, 1));
                // UUID 0,1 is a convention for "plan run" instead of "chain run".

                for (Place p : s.getRunsOn().getPlaces())
                {
                    pj.setPlace(p);
                    pj.setLevel0Id(s.getChain().getId());
                    Event e = pj.createEvent();

                    // Send the event
                    try
                    {
                        SenderHelpers.sendEvent(e, eventProducer, jmsSession, ctx, false);
                    }
                    catch (JMSException ex)
                    {
                        throw new ChronixRunException("Cannot create clock events. A clock has failed.", ex);
                    }

                    log.debug(String.format("Creating event on place %s for state [%s in chain %s]", p.getName(), this.name, s.getChain()
                            .getName()));

                    // Mark the tick as done
                    ClockTick ct = new ClockTick();
                    ct.setAppId(this.application.getId());
                    ct.setClockId(this.id);
                    ct.setTickTime(dt);
                    ct.insert(conn);
                }
            }
        }

        // Purge the past ticks
        conn.createQuery("DELETE FROM ClockTick t WHERE t.tickTime < :before").
                addParameter("before", nowminusgrace.toDate()).executeUpdate();
        //TODO: check we don't need to filter purge by clock ??????

        // Get the next time the method should be called and return it
        org.joda.time.DateTime res = virtualTime.plusDays(1).minusMillis(virtualTime.getMillisOfDay());
        for (Object p : occurrenceCache)
        {
            org.joda.time.DateTime from = new org.joda.time.DateTime(((Period) p).getStart());
            if (from.compareTo(virtualTime) > 0)
            {
                res = new org.joda.time.DateTime(from.toDate());
                break;
            }
        }
        log.debug(String.format("The clock asks to be awoken at %s", res.toString(LOG_DATE_FORMAT)));
        return res;
    }
    //
    // /////////////////////////////////////////////////////////////////////

}
