package org.oxymores.chronix.core;

import java.sql.Date;
import java.util.ArrayList;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.transactional.CalendarPointer;

public class Calendar extends ApplicationObject {
	private static Logger log = Logger.getLogger(Calendar.class);

	public boolean isManualSequence() {
		return manualSequence;
	}

	public void setManualSequence(boolean manualSequence) {
		this.manualSequence = manualSequence;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setName(String name) {
		this.name = name;
	}

	private static final long serialVersionUID = 7332812989443095188L;

	protected boolean manualSequence;
	protected String name;
	protected String description;

	protected ArrayList<State> usedInStates;
	protected ArrayList<CalendarDay> days;

	public Calendar() {
		super();
		usedInStates = new ArrayList<State>();
		days = new ArrayList<CalendarDay>();
	}

	// Only called from State.addSequence
	void s_addStateUsing(State s) {
		usedInStates.add(s);
	}

	// Only called from State.addSequence
	void s_removeStateUsing(State s) {
		try {
			usedInStates.remove(s);
		} finally { // do nothing if asked to remove a non existent state
		}
	}

	public String getName() {
		return this.name;
	}

	public void addDay(Date d) {
		addDay(new CalendarDay(d.toString(), this));
	}

	public void addDay(String occurrenceName) {
		addDay(new CalendarDay(occurrenceName, this));
	}

	public void addDay(CalendarDay d) {
		if (!this.days.contains(d)) {
			this.days.add(d);
			d.setCalendar(this);
		}
	}

	public ArrayList<CalendarDay> getCalendarDays() {
		return this.days;
	}

	public CalendarDay getDay(UUID id) {
		for (CalendarDay cd : this.days) {
			if (cd.id.equals(id))
				return cd;
		}
		return null;
	}

	public CalendarDay getCurrentOccurrence(EntityManager em) {
		// Calendar current occurrence pointers have no states and places: they
		// are only related to the calendar itself.
		Query q = em
				.createQuery("SELECT p FROM CalendarPointer p WHERE p.stateID IS NULL AND p.placeID IS NULL AND p.calendarID = ?1");
		q.setParameter(1, this.id.toString());
		CalendarPointer cp = (CalendarPointer) q.getSingleResult();
		log.debug(String.format(
				"get current occurrence pointer found: %s. last ok id: %s", cp,
				cp.getLastEndedOkOccurrenceId()));
		log.debug(String.format("get current occurrence corresponding day: %s",
				this.getDay(cp.getLastEndedOkOccurrenceUuid())));
		return this.getDay(cp.getLastEndedOkOccurrenceUuid());
	}

	public CalendarDay getOccurrenceAfter(CalendarDay d) {
		return getOccurrenceShiftedBy(d, 1);
	}

	public CalendarDay getOccurrenceShiftedBy(CalendarDay origin, int shift) {
		return this.days.get(this.days.indexOf(origin) + shift);
	}

	public CalendarDay getFirstOccurrence() {
		return this.days.get(0);
	}

	public Boolean isBeforeOrSame(CalendarDay before, CalendarDay after) {
		return this.days.indexOf(before) <= this.days.indexOf(after);
	}

	// Called within an open transaction. Won't be committed here.
	public void createPointers(EntityManager em) {
		// Get existing pointers
		try {
			getCurrentOccurrence(em);
			return;
		} catch (NoResultException e) {
		}

		log.info(String
				.format("Calendar %s current value will be initialised at its first occurrence: %s - %s",
						this.name, this.getFirstOccurrence().getValue(), this
								.getFirstOccurrence().getId()));

		CalendarPointer tmp = new CalendarPointer();
		tmp.setApplication(this.application);
		tmp.setCalendar(this);
		tmp.setLastEndedOkOccurrenceCd(this.getFirstOccurrence());
		tmp.setLastEndedOccurrenceCd(this.getFirstOccurrence());
		tmp.setLastStartedOccurrenceCd(this.getFirstOccurrence());
		tmp.setPlace(null);
		tmp.setState(null);

		em.persist(tmp);

		// Commit is done by the calling method
	}
}
