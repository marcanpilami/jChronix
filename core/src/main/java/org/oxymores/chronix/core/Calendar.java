package org.oxymores.chronix.core;

import java.sql.Date;
import java.util.ArrayList;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.oxymores.chronix.core.transactional.CalendarPointer;

public class Calendar extends ApplicationObject {

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
			if (cd.id == id)
				return cd;
		}
		return null;
	}

	public CalendarDay getCurrentOccurrence(EntityManager em) {
		// Calendar current occurrence pointers have no states and places: they
		// are only related to the calendar itself.
		Query q = em
				.createQuery("SELECT e FROM CalendarPointer p WHERE p.stateID IS NULL AND p.placeID IS NULL AND p.calendarId = ?1");
		q.setParameter(1, this.id.toString());
		CalendarPointer cp = (CalendarPointer) q.getSingleResult();
		return this.getDay(cp.getLastOkOccurrenceUuid());
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
}
