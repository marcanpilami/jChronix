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
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.demo.DemoApplication;

public class TestClock {
	private static Logger log = Logger.getLogger(TestClock.class);

	private void displayLogPeriods(PeriodList pl) {
		for (Object p : pl) {
			DateTime from = new DateTime(((Period) p).getStart());
			DateTime to = new DateTime(((Period) p).getEnd());
			log.info(String.format("from %s to %s",
					from.toString("yyyy/MM/dd HH:mm:ss"),
					to.toString("yyyy/MM/dd HH:mm:ss")));
		}
	}

	@Test
	public void testSimpleRec() throws ParseException {
		log.info("********** Testing basic reccurrences with clocks");

		Application a = DemoApplication.getNewDemoApplication();
		Clock ck1 = a.getActiveElements(Clock.class).get(0);
		ClockRRule rr1 = ck1.getRulesADD().get(0);

		Date start = new DateTime(2012, 8, 6, 0, 0).toDate(); // a monday
		Date end = new DateTime(2012, 8, 12, 0, 0).toDate(); // sunday
		
		// As in the demo application: on open days only
		log.info("**** Normal");
		PeriodList pl = ck1.getOccurrences(start, end);
		displayLogPeriods(pl);
		Assert.assertEquals(5, pl.size());

		// Add a second occurrence per day
		log.info("**** Twice a day");
		rr1.setBYHOUR("05,10");
		pl = ck1.getOccurrences(start, end);
		displayLogPeriods(pl);
		Assert.assertEquals(10, pl.size());

		// Add exception
		log.info("**** With the same rule as an exception");
		ck1.addRRuleEXC(rr1);
		pl = ck1.getOccurrences(start, end);
		displayLogPeriods(pl);
		Assert.assertEquals(0, pl.size());
		
		// Test we can remove a rule
		ck1.removeRRuleEXC(rr1);
		pl = ck1.getOccurrences(start, end);
		displayLogPeriods(pl);
		Assert.assertEquals(10, pl.size());
		
		ck1.removeRRuleADD(rr1);
		pl = ck1.getOccurrences(start, end);
		displayLogPeriods(pl);
		Assert.assertEquals(0, pl.size());
	}
}
