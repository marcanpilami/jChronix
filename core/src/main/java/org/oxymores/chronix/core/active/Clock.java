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

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.log4j.Logger;
import org.joda.time.Interval;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.ClockTick;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

public class Clock extends ActiveNodeBase
{
	private static final long serialVersionUID = -5203055591135192345L;
	private static Logger log = Logger.getLogger(Clock.class);

	// Fields
	org.joda.time.DateTime CREATED;
	int DURATION = 0; // Minutes

	// Relationships
	ArrayList<ClockRRule> rulesADD, rulesEXC;

	// Helpers for engine methods
	transient PeriodList occurrenceCache;
	transient org.joda.time.DateTime lastComputed;
	private PipelineJob pj;

	// /////////////////////////////////////////////////////////////////////
	// Constructor
	public Clock()
	{
		rulesADD = new ArrayList<ClockRRule>();
		rulesEXC = new ArrayList<ClockRRule>();
		CREATED = org.joda.time.DateTime.now();
		CREATED = CREATED.minusMillis(CREATED.getMillisOfSecond());
		CREATED = CREATED.minusSeconds(CREATED.getSecondOfMinute());

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

	//
	// /////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////
	// iCal stuff
	private VEvent getEvent() throws ParseException
	{
		VEvent evt = new VEvent();

		// evt.getProperties().add(new DtStart(new DateTime(this.CREATED.toDate())));
		// evt.getProperties().add(new DtEnd(new DateTime(this.CREATED.plusYears(1).toDate()))); // DEBUG
		evt.getProperties().add(new net.fortuna.ical4j.model.property.Duration(new Dur(0, 0, this.DURATION, 0)));

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

		log.debug(String.format("Computing ocurrences from %s to %s.", from, to));
		VEvent evt = this.getEvent();

		evt.getProperties().add(new DtStart(new DateTime(start.minusDays(1).toDate())));
		// evt.getProperties().add(new DtEnd(new DateTime(end.plusDays(5).toDate())));

		log.debug(String.format("Event start time is %s - creation is %s", evt.getStartDate(), evt.getCreated()));
		Period p = new Period(from, to);
		PeriodList res = evt.calculateRecurrenceSet(p);
		return res;
	}

	//
	// /////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////
	// Stupid GET/SET
	public int getDURATION()
	{
		return DURATION;
	}

	public void setDURATION(int dURATION)
	{
		DURATION = dURATION;
	}

	public org.joda.time.DateTime getCREATED()
	{
		return CREATED;
	}

	public ArrayList<ClockRRule> getRulesADD()
	{
		return rulesADD;
	}

	public ArrayList<ClockRRule> getRulesEXC()
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
			rulesADD.add(rule);
	}

	public void removeRRuleADD(ClockRRule rule)
	{
		if (rulesADD.contains(rule))
			rulesADD.remove(rule);
	}

	public void addRRuleEXC(ClockRRule rule)
	{
		if (!rulesEXC.contains(rule))
			rulesEXC.add(rule);
	}

	public void removeRRuleEXC(ClockRRule rule)
	{
		if (rulesEXC.contains(rule))
			rulesEXC.remove(rule);
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
	public org.joda.time.DateTime selfTrigger(MessageProducer eventProducer, Session jmsSession, ChronixContext ctx, EntityManager em,
			org.joda.time.DateTime present) throws Exception
	{
		org.joda.time.DateTime now = org.joda.time.DateTime.now();
		if ((now.compareTo(present) >= 0 && (new Interval(present, now)).toDurationMillis() > 1000)
				|| (now.compareTo(present) < 0 && (new Interval(now, present)).toDurationMillis() > 1000))
			log.warn("There is more than one second between internal time and clock time - performance issue? (discard if simulation)");
		now = present;
		org.joda.time.DateTime nowminusgrace = now.minusMinutes(this.DURATION);

		if (occurrenceCache == null || lastComputed == null || lastComputed.getDayOfYear() < now.getDayOfYear())
		{
			occurrenceCache = this.getOccurrences(nowminusgrace, now.plusDays(1));
			log.debug(String.format("%s ocurrences were added to cache", occurrenceCache.size()));
		}

		// Select the occurrences that should be active
		ArrayList<org.joda.time.DateTime> theory = new ArrayList<org.joda.time.DateTime>();
		for (Object p : occurrenceCache)
		{
			org.joda.time.DateTime from = new org.joda.time.DateTime(((Period) p).getStart());
			org.joda.time.DateTime to = new org.joda.time.DateTime(((Period) p).getEnd());

			if (from.compareTo(now) <= 0 && to.compareTo(now) >= 0)
			{
				theory.add(from);
				log.trace(from.toString("dd/MM/YYYY HH:mm:ss") + " - " + to.toString("dd/MM/YYYY HH:mm:ss"));
			}
		}
		log.debug(String.format("There are %s clock ticks that should be active at %s", theory.size(), now.toString("dd/MM/YYYY HH:mm:ss")));

		// Select the ones that are active
		TypedQuery<ClockTick> q = em.createQuery("SELECT t FROM ClockTick t WHERE t.TickTime >= ?1 ORDER BY t.TickTime", ClockTick.class);
		q.setParameter(1, nowminusgrace.toDate());
		List<ClockTick> real = q.getResultList();
		log.debug(String.format("There are %s clock ticks that really are active", real.size()));

		// Select the ones that will have to be created
		List<org.joda.time.DateTime> toCreate = theory.subList(Math.min(real.size(), theory.size()), theory.size());
		log.debug(String.format("%s ticks will have to be created", toCreate.size()));

		// //////////////////////////
		// Create events

		// Get all states that use this clock
		ArrayList<State> states = this.getClientStates();

		// Create events through the helper PJ
		for (org.joda.time.DateTime dt : toCreate)
		{
			for (State s : states)
			{
				pj.setState(s);
				pj.setLevel1IdU(new UUID(0, 1)); // convention for plan unique
													// instance
				for (Place p : s.getRunsOn().getPlaces())
				{
					pj.setPlace(p);
					pj.setLevel0IdU(s.getChain().getId());
					Event e = pj.createEvent();

					// Send the event
					SenderHelpers.sendEvent(e, eventProducer, jmsSession, ctx, false);

					log.debug(String.format("Creating event on place %s for state [%s in chain %s]", p.getName(), this.name, s.getChain()
							.getName()));

					// Mark the tick as done
					ClockTick ct = new ClockTick();
					ct.ClockId = this.id.toString();
					ct.TickTime = dt.toDate();
					em.persist(ct);
				}
			}
		}

		// Purge the past ticks
		q = em.createQuery("SELECT t FROM ClockTick t WHERE t.TickTime < ?1 ORDER BY t.TickTime", ClockTick.class);
		q.setParameter(1, nowminusgrace.toDate());
		real = q.getResultList();
		for (ClockTick ct : real)
			em.remove(ct);

		// Get the next time the method should be called and return it
		org.joda.time.DateTime res = now.plusDays(1).minusMillis(now.getMillisOfDay());
		for (Object p : occurrenceCache)
		{
			org.joda.time.DateTime from = new org.joda.time.DateTime(((Period) p).getStart());
			if (from.compareTo(now) > 0)
			{
				res = new org.joda.time.DateTime(from.toDate());
				break;
			}
		}
		log.debug(String.format("The clock asks to be awoken at %s", res.toString("dd/MM/YYYY hh:mm:ss")));
		return res;
	}
	//
	// /////////////////////////////////////////////////////////////////////

}
