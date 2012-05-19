package org.oxymores.chronix.core;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.UUID;

public class Calendar extends ApplicationObject {

	private static final long serialVersionUID = 7332812989443095188L;

	protected boolean manualSequence;
	protected String name;
	protected String description;

	protected ArrayList<State> usedInStates;
	protected Hashtable<UUID, CalendarDay> days;

	public Calendar() {
		super();
		usedInStates = new ArrayList<State>();
		days = new Hashtable<UUID, CalendarDay>();
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

	public void addDay(Date d) {
		addDay(new CalendarDay(d.getTime(), this));
	}

	public void addDay(long d) {
		addDay(new CalendarDay(d, this));
	}

	public void addDay(CalendarDay d) {
		if (!this.days.contains(d)) {
			this.days.put(d.id, d);
			d.setCalendar(this);
		}
	}

	public Hashtable<UUID, CalendarDay> getCalendarDays() {
		return this.days;
	}

	public CalendarDay getDay(UUID id) {
		return this.days.get(id);
	}
}
