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
	protected String lastOkOccurrenceId; // Updated at end of run
	protected String lastStartedOccurrenceId; // Updated before run
	protected String lastEndedOccurrenceId; // Updated after run

	public Boolean isEnd = false;
	public Boolean latestFailed = false;

	// //////////////////////////////////////////////
	// Helper fields
	public Boolean getIsEnd() {
		return isEnd;
	}

	public void setIsEnd(Boolean isEnd) {
		this.isEnd = isEnd;
	}

	public Boolean getLatestFailed() {
		return latestFailed;
	}

	public void setLatestFailed(Boolean latestFailed) {
		this.latestFailed = latestFailed;
	}

	//
	// //////////////////////////////////////////////

	// //////////////////////////////////////////////
	// Calendar itself
	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String id) {
		this.calendarId = id;
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

	//
	// //////////////////////////////////////////////

	// //////////////////////////////////////////////
	// Last day it ended correctly
	public String getLastOkOccurrenceId() {
		return lastOkOccurrenceId;
	}

	public UUID getLastOkOccurrenceUuid() {
		return UUID.fromString(lastOkOccurrenceId);
	}

	public void setLastOkOccurrenceId(String dayId) {
		this.lastOkOccurrenceId = dayId;
	}

	public CalendarDay getLastOkOccurrenceCd(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(
				UUID.fromString(this.lastOkOccurrenceId));
	}

	public void setLastOkOccurrenceCd(CalendarDay day) {
		if (day == null) {
			this.lastOkOccurrenceId = null;
		} else {
			this.lastOkOccurrenceId = day.getId().toString();
		}
	}

	//
	// //////////////////////////////////////////////

	// //////////////////////////////////////////////
	// Last day it was started
	public String getLastStartedOccurrenceId() {
		return lastStartedOccurrenceId;
	}

	public UUID getLastStartedOccurrenceUuid() {
		return UUID.fromString(lastStartedOccurrenceId);
	}

	public void setLastStartedOccurrenceId(String dayId) {
		this.lastStartedOccurrenceId = dayId;
	}

	public CalendarDay getLastStartedOccurrenceCd(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(
				UUID.fromString(this.lastStartedOccurrenceId));
	}

	public void setLastStartedOccurrenceCd(CalendarDay day) {
		if (day == null) {
			this.lastStartedOccurrenceId = null;
		} else {
			this.lastStartedOccurrenceId = day.getId().toString();
		}
	}

	//
	// //////////////////////////////////////////////

	// //////////////////////////////////////////////
	// Last day it finished (possibly incorrectly)
	public String getLastEndedOccurrenceId() {
		return lastEndedOccurrenceId;
	}

	public UUID getLastEndedOccurrenceUuid() {
		return UUID.fromString(lastEndedOccurrenceId);
	}

	public void setLastEndedOccurrenceId(String dayId) {
		this.lastEndedOccurrenceId = dayId;
	}

	public CalendarDay getLastEndedOccurrenceCd(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(
				UUID.fromString(this.lastEndedOccurrenceId));
	}

	public void setLastEndedOccurrenceCd(CalendarDay day) {
		if (day == null) {
			this.lastEndedOccurrenceId = null;
		} else {
			this.lastEndedOccurrenceId = day.getId().toString();
		}
	}
	//
	// //////////////////////////////////////////////

}
