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

package org.oxymores.chronix.core;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.transactional.CalendarPointer;

public class Calendar extends ApplicationObject
{
	private static Logger log = Logger.getLogger(Calendar.class);
	private static final long serialVersionUID = 7332812989443095188L;

	protected boolean manualSequence;
	protected String name;
	protected String description;
	protected Integer alertThreshold = 20;

	protected ArrayList<State> usedInStates;
	protected ArrayList<CalendarDay> days;

	protected ClockRRule createdFrom = null; // Optional
	protected boolean autoReset = false;

	// Constructor
	public Calendar()
	{
		super();
		usedInStates = new ArrayList<State>();
		days = new ArrayList<CalendarDay>();
	}

	// ///////////////////////////////////////////////////////////////
	// Stupid get/set

	public boolean isManualSequence()
	{
		return manualSequence;
	}

	public void setManualSequence(boolean manualSequence)
	{
		this.manualSequence = manualSequence;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return this.name;
	}

	public Integer getAlertThreshold()
	{
		return this.alertThreshold;
	}

	public List<State> getUsedInStates()
	{
		return this.usedInStates;
	}

	public boolean isAutoReset()
	{
		return autoReset;
	}

	public void setAutoReset(boolean autoReset)
	{
		this.autoReset = autoReset;
	}

	public void setAlertThreshold(Integer alertThreshold)
	{
		this.alertThreshold = alertThreshold;
	}

	//
	// ///////////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////////
	// Setters on relationships

	// Only called from State.addSequence
	void s_addStateUsing(State s)
	{
		usedInStates.add(s);
	}

	// Only called from State.addSequence
	void s_removeStateUsing(State s)
	{
		try
		{
			usedInStates.remove(s);
		} finally
		{ // do nothing if asked to remove a non existent state
		}
	}

	public void addDay(Date d)
	{
		addDay(new CalendarDay(d.toString(), this));
	}

	public void addDay(String occurrenceName)
	{
		addDay(new CalendarDay(occurrenceName, this));
	}

	public void addDay(CalendarDay d)
	{
		if (!this.days.contains(d))
		{
			this.days.add(d);
			d.setCalendar(this);
		}
	}

	public ArrayList<CalendarDay> getCalendarDays()
	{
		return this.days;
	}

	public CalendarDay getOccurrence(String sequenceValue)
	{
		for (CalendarDay cd : this.days)
			if (cd.seq.equals(sequenceValue))
				return cd;
		return null;
	}

	public CalendarDay getDay(UUID id)
	{
		for (CalendarDay cd : this.days)
		{
			if (cd.id.equals(id))
				return cd;
		}
		return null;
	}

	public ClockRRule getCreatedFrom()
	{
		return createdFrom;
	}

	public void setCreatedFrom(ClockRRule createdFrom)
	{
		this.createdFrom = createdFrom;
	}

	//
	// ///////////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////////
	// Operational data

	public CalendarPointer getCurrentOccurrencePointer(EntityManager em)
	{
		// Calendar current occurrence pointers have no states and places: they
		// are only related to the calendar itself.
		Query q = em.createQuery("SELECT p FROM CalendarPointer p WHERE p.stateID IS NULL AND p.placeID IS NULL AND p.calendarID = ?1");
		q.setParameter(1, this.id.toString());
		CalendarPointer cp = (CalendarPointer) q.getSingleResult();
		em.refresh(cp);
		return cp;
	}

	public CalendarDay getCurrentOccurrence(EntityManager em)
	{
		return this.getDay(this.getCurrentOccurrencePointer(em).getLastEndedOkOccurrenceUuid());
	}

	public CalendarDay getOccurrenceAfter(CalendarDay d)
	{
		return getOccurrenceShiftedBy(d, 1);
	}

	public CalendarDay getOccurrenceShiftedBy(CalendarDay origin, int shift)
	{
		return this.days.get(this.days.indexOf(origin) + shift);
	}

	public CalendarDay getFirstOccurrence()
	{
		return this.days.get(0);
	}

	public Boolean isBeforeOrSame(CalendarDay before, CalendarDay after)
	{
		return this.days.indexOf(before) <= this.days.indexOf(after);
	}

	public Boolean isBefore(CalendarDay before, CalendarDay after)
	{
		return this.days.indexOf(before) < this.days.indexOf(after);
	}

	// Called within an open transaction. Won't be committed here.
	public void createPointers(EntityManager em)
	{
		// Get existing pointers
		try
		{
			getCurrentOccurrence(em);
			return;
		} catch (NoResultException e)
		{
		}

		int minShift = 0;
		for (State s : usedInStates)
		{
			if (s.calendarShift < minShift)
				minShift = s.calendarShift;
		}
		minShift--;

		CalendarDay cd = this.getOccurrenceShiftedBy(this.getFirstOccurrence(), -minShift);
		log.info(String
				.format("Calendar %s current value will be initialised at its first allowed occurrence by the shifts of the using states (max shift is %s): %s - %s",
						this.name, minShift, cd.getValue(), cd.getId()));

		CalendarPointer tmp = new CalendarPointer();
		tmp.setApplication(this.application);
		tmp.setCalendar(this);
		tmp.setLastEndedOkOccurrenceCd(cd);
		tmp.setLastEndedOccurrenceCd(cd);
		tmp.setLastStartedOccurrenceCd(cd);
		tmp.setPlace(null);
		tmp.setState(null);

		em.persist(tmp);

		// Commit is done by the calling method
	}

	//
	// ///////////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////////
	// Don't go overboard...

	public boolean warnNotEnoughOccurrencesLeft(EntityManager em)
	{
		CalendarDay cd = this.getCurrentOccurrence(em);
		int onow = this.days.indexOf(cd);

		return onow + this.alertThreshold >= this.days.size();
	}

	public boolean errorNotEnoughOccurrencesLeft(EntityManager em)
	{
		CalendarDay cd = this.getCurrentOccurrence(em);
		int onow = this.days.indexOf(cd);

		return onow + this.alertThreshold / 2 >= this.days.size();
	}

	//
	// ///////////////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////////////
	// Stragglers
	public class StragglerIssue
	{
		public State s;
		public Place p;
	}

	public List<StragglerIssue> getStragglers(EntityManager em)
	{
		List<StragglerIssue> issues = new ArrayList<StragglerIssue>();
		StragglerIssue tmp = null;

		for (State s : this.usedInStates)
		{
			for (Place p : s.getRunsOn().getPlaces())
			{
				if (s.isLate(em, p))
				{
					tmp = new StragglerIssue();
					tmp.p = p;
					tmp.s = s;
					issues.add(tmp);
				}
			}
		}
		return issues;
	}

	public void processStragglers(EntityManager em) throws Exception
	{
		log.debug(String.format("Processing stragglers on calendar %s", this.name));
		CalendarDay d = this.getCurrentOccurrence(em);
		for (StragglerIssue i : getStragglers(em))
		{
			log.warn(String
					.format("State %s on place %s (in chain %s) is now late according to its calendar: it has only finished %s while it should be ready to run %s shifted by %s",
							i.s.represents.name, i.p.name, i.s.chain.name, i.s.getCurrentCalendarOccurrence(em, i.p).seq, d.seq,
							i.s.calendarShift));
		}
	}

	//
	// ///////////////////////////////////////////////////////////////

	public ArrayList<CalendarDay> getDays()
	{
		return days;
	}

}
