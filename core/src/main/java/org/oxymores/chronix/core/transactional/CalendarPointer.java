package org.oxymores.chronix.core.transactional;

import java.util.UUID;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;

public class CalendarPointer extends TranscientBase {

	private static final long serialVersionUID = 6905957323594389673L;

	protected UUID calendarId, dayId, appId;

	public UUID getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(UUID id) {
		this.calendarId = id;
	}

	public Application getApplication(ChronixContext ctx) {
		return ctx.applicationsById.get(this.appID);
	}

	public void setApplication(Application application) {
		if (application != null)
			this.appID = application.getId();
		else
			this.appID = null;
	}

	public UUID getDayId() {
		return dayId;
	}

	public void setDayId(UUID dayId) {
		this.dayId = dayId;
	}

	public Calendar getCalendar(ChronixContext ctx) {
		return this.getApplication(ctx).getCalendar(this.calendarId);
	}

	public void setCalendar(Calendar c) {
		if (c == null) {
			this.calendarId = null;
		} else {
			this.calendarId = c.getId();
		}
	}

	public CalendarDay getCalendarDay(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(this.dayId);
	}

	public void setCalendarDay(CalendarDay day) {
		if (day == null) {
			this.dayId = null;
		} else {
			this.dayId = day.getId();
		}
	}

	public UUID getAppId() {
		return appId;
	}

	public void setAppId(UUID appId) {
		this.appId = appId;
	}
}
