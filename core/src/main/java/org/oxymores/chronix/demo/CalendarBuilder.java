package org.oxymores.chronix.demo;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;

public class CalendarBuilder {

	public static Calendar buildWorkDayCalendar(Application a, int year) {
		Calendar cal1 = new Calendar();
		cal1.setName("Week days");
		cal1.setDescription("All days from monday to friday for the whole year");
		cal1.setManualSequence(false);
		a.addCalendar(cal1);

		DateTime d = new DateTime(year, 1, 1, 0, 0);
		while (d.getYear() == year) {
			if (d.getDayOfWeek() <= 5)
				new CalendarDay(d.toString("dd/MM/yyyy"), cal1);
			d = d.plusDays(1);
		}

		return cal1;
	}

	public static Calendar buildWeekDayCalendar(Application a, int year) {
		Calendar cal1 = new Calendar();
		cal1.setName("Week days");
		cal1.setDescription("All days from monday to friday for the whole year");
		cal1.setManualSequence(false);
		a.addCalendar(cal1);

		DateTime d = new DateTime(year, 1, 1, 0, 0);
		while (d.getYear() == year) {
			new CalendarDay(d.toString("dd/MM/yyyy"), cal1);
			d = d.plusDays(1);
		}

		return cal1;
	}
}
