package org.oxymores.chronix.engine;

import java.text.ParseException;
import java.util.Date;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.demo.DemoApplication;

public class TestClock {
	private static Logger log = Logger.getLogger(TestClock.class);

	@SuppressWarnings("deprecation")
	@Test
	public void testSimpleRec() throws ParseException {
		log.info("********** Testing week days");

		Application a = DemoApplication.getNewDemoApplication();

		Clock ck1 = a.getActiveElements(Clock.class).get(0);

		Date start = new DateTime(2012, 8, 6, 0, 0).toDate(); // a monday
		Date end = new DateTime(2012, 8, 12, 0, 0).toDate(); // sunday
		PeriodList pl = ck1.getOccurrences(start, end);

		for (Object p : pl) {
			log.info(((Period) p).getStart().toLocaleString());
		}

		Assert.assertEquals(5, pl.size());
	}
}
