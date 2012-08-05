package org.oxymores.chronix.core.transactional;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.ChronixContext;

@Entity
public class CalendarPointer extends TranscientBase {

	private static final long serialVersionUID = 6905957323594389673L;

	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String lastEndedOkOccurrenceId; // Updated at end of run
	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String lastStartedOccurrenceId; // Updated before run
	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String lastEndedOccurrenceId; // Updated after run
	@Column(columnDefinition = "CHAR(36)", length = 36)
	protected String nextRunOccurrenceId;

	public Boolean latestFailed = false;
	public Boolean running = false;

	// //////////////////////////////////////////////
	// Helper fields
	public Boolean getRunning() {
		return running;
	}

	public void setRunning(Boolean running) {
		this.running = running;
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
	// Last day it ended correctly
	public String getLastEndedOkOccurrenceId() {
		return lastEndedOkOccurrenceId;
	}

	public UUID getLastEndedOkOccurrenceUuid() {
		return UUID.fromString(lastEndedOkOccurrenceId);
	}

	public void setLastEndedOkOccurrenceId(String dayId) {
		this.lastEndedOkOccurrenceId = dayId;
	}

	public CalendarDay getLastEndedOkOccurrenceCd(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(
				UUID.fromString(this.lastEndedOkOccurrenceId));
	}

	public void setLastEndedOkOccurrenceCd(CalendarDay day) {
		if (day == null) {
			this.lastEndedOkOccurrenceId = null;
		} else {
			this.lastEndedOkOccurrenceId = day.getId().toString();
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

	// //////////////////////////////////////////////
	// Next time it will run, it will be...
	public String getNextRunOccurrenceId() {
		return nextRunOccurrenceId;
	}

	public UUID getNextRunOccurrenceUuid() {
		return UUID.fromString(nextRunOccurrenceId);
	}

	public void setNextRunOccurrenceId(String dayId) {
		this.nextRunOccurrenceId = dayId;
	}

	public CalendarDay getNextRunOccurrenceCd(ChronixContext ctx) {
		return this.getCalendar(ctx).getDay(
				UUID.fromString(this.nextRunOccurrenceId));
	}

	public void setNextRunOccurrenceCd(CalendarDay day) {
		if (day == null) {
			this.nextRunOccurrenceId = null;
		} else {
			this.nextRunOccurrenceId = day.getId().toString();
		}
	}
	//
	// //////////////////////////////////////////////

}
