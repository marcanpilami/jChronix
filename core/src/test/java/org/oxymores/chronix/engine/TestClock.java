package org.oxymores.chronix.engine;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.demo.DemoApplication;
import org.oxymores.chronix.demo.PlanBuilder;

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

		DateTime start = new DateTime(2012, 8, 6, 0, 0); // a monday
		DateTime end = new DateTime(2012, 8, 12, 0, 0); // sunday

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

	@Test
	public void testClockTrigger() throws Exception {
		String dbPath = "C:\\TEMP\\db1";
		ChronixEngine e = new ChronixEngine(dbPath);
		e.emptyDb();
		e.injectListenerConfigIntoDb();

		// Create test application
		Application a = PlanBuilder.buildApplication("testing clocks",
				"no description for tests");
		PlaceGroup pgLocal = PlanBuilder.buildDefaultLocalNetwork(a);
		Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);

		ClockRRule rr1 = PlanBuilder.buildRRule10Seconds(a);
		Clock ck1 = PlanBuilder.buildClock(a, "every second", "every second",
				rr1);
		ck1.setDURATION(1);
		ShellCommand sc1 = PlanBuilder.buildNewActiveShell(a, "echo aa", "aa",
				"should display 'aa'");

		State s1 = PlanBuilder.buildNewState(c, pgLocal, ck1);
		State s2 = PlanBuilder.buildNewState(c, pgLocal, sc1);
		s1.connectTo(s2);

		ChronixContext ctx = new ChronixContext();
		ctx.configurationDirectory = new File(dbPath);
		ctx.saveApplication(a);
		ctx.setWorkingAsCurrent(a);

		// Wait until the next x1 second.
		DateTime now = DateTime.now();
		int toAdd = 11 - now.getSecondOfMinute() % 10;
		Thread.sleep(toAdd * 1000);

		// Start engine
		e.start();
		Thread.sleep(3 * 1000); // run & analysis time
		e.stop();
		Thread.sleep(500); // In case we chain tests - free TCP port

		// Get results
		List<RunLog> res = LogHelpers.displayAllHistory();

		// 6 per minute - grace period is 1 minute => 6 runs
		Assert.assertEquals(6, res.size());
	}
}
