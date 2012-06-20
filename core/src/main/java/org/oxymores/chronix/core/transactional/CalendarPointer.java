package org.oxymores.chronix.core.transactional;

import java.util.UUID;

import javax.persistence.Entity;

import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;

@Entity
public class CalendarPointer extends TranscientBase {

	private static final long serialVersionUID = 6905957323594389673L;

	protected String calendarId;
	protected String dayId;

	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String id) {
		this.calendarId = id;
	}

	public String getDayId() {
		return dayId;
	}

	public void setDayId(String dayId) {
		this.dayId = dayId;
	}

	public Calendar getCalendar(ChronixContext ctx) {
		return this.getApplication(ctx).getCalendar(
				UUID.fromString(this.calendarId));
	}

	public void setCalendar(Calendar c) {
		if (c == null) {
			this.calendarId = null;
		} else {
			this.calendarId = c.getId().toString();
		}
	}

	public CalendarDay getCalendarDay(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(UUID.fromString(this.dayId));
	}

	public void setCalendarDay(CalendarDay day) {
		if (day == null) {
			this.dayId = null;
		} else {
			this.dayId = day.getId().toString();
		}
	}
}
