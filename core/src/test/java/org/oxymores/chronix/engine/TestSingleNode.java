package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.demo.CalendarBuilder;
import org.oxymores.chronix.demo.PlanBuilder;

public class TestSingleNode {
	private static Logger log = Logger.getLogger(TestSingleNode.class);

	private String db1;
	Application a1;
	ChronixEngine e1;
	ExecutionNode en1;
	Place p1;
	PlaceGroup pg1, pg2;

	@After
	public void cleanup() {
		log.debug("**************************************************************************************");
		log.debug("****END OF TEST***********************************************************************");
		if (e1 != null && e1.run) {
			e1.stopEngine();
			e1.waitForStopEnd();
		}
	}

	@Before
	public void prepare() throws Exception {
		db1 = "C:\\TEMP\\db1";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1);
		e1.emptyDb();
		e1.ctx.createNewConfigFile(); // default is 1789/localhost
		LogHelpers.clearAllTranscientElements(e1.ctx);

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Single node test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, 1789);
		en1.setConsole(true);

		// Logical network
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1);

		// Chains and other stuff depends on the test

		// Save app in node 1
		try {
			e1.ctx.saveApplication(a1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try {
			e1.ctx.setWorkingAsCurrent(a1);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		/************************************************
		 * Start the engine
		 ***********************************************/

		e1.start();
		log.debug("Started - begin waiting");
		e1.waitForInitEnd();
		log.debug("Engines inits done");
	}

	@Test
	public void mainScenario() throws Exception {
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();
		Calendar ca1 = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain using calendar", "chain2", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		s1.setCalendar(ca1);
		s1.setCalendarShift(-1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
		NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca1);
		State s2 = PlanBuilder.buildState(c2, pg1, no);
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo ahahaha", "echo hahaha", "oo");
		State s3 = PlanBuilder.buildState(c2, pg1, sc2);
		s3.setCalendar(ca1);

		// Save plan
		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Start chain
		log.debug("**************************************************************************************");
		log.debug("****FIRST (PASSING) RUN***************************************************************");
		SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		Thread.sleep(2000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(3, res.size());

		// Now, launch again. Should block after echo, for the calendar has not
		// progressed.
		log.debug("**************************************************************************************");
		log.debug("****SECOND (BLOCKING - CALENDAR) RUN**************************************************");
		SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		Thread.sleep(2000); // Time to consume message

		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(4, res.size());

		EntityManager em2 = e1.ctx.getTransacEM();
		TypedQuery<CalendarPointer> q2 = em2.createQuery("SELECT r FROM CalendarPointer r", CalendarPointer.class);
		for (CalendarPointer c : q2.getResultList()) {
			log.debug(c.getRunning());
		}

		TypedQuery<Event> q3 = em2.createQuery("SELECT e FROM Event e", Event.class);
		List<Event> events = q3.getResultList();
		Assert.assertEquals(1, events.size()); // purge - only pending remain

		// Now, advance calendar
		log.debug("**************************************************************************************");
		log.debug("****CALENDAR UPDATE*******************************************************************");
		SenderHelpers.runStateInsidePlan(s2, e1.ctx, em);
		Thread.sleep(1000);

		// Test the event has been reanalyzed
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(7, res.size());

		// and do it again: the end of chain1 should not run.
		log.debug("**************************************************************************************");
		log.debug("****THIRD (BLOCKING - CALENDAR) RUN***************************************************");
		SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		Thread.sleep(2000); // Time to consume message

		// Test...
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(8, res.size());

		// and finally free the calendar, and test that state s3 is considered
		// as straggling
		log.debug("**************************************************************************************");
		log.debug("****CALENDAR UPDATE*******************************************************************");
		SenderHelpers.runStateInsidePlan(s2, e1.ctx, em);
		Thread.sleep(2000);

		// Test stragglers
		ca1.processStragglers(em2); // Display to ease debug
		Assert.assertEquals(1, ca1.getStragglers(em2).size());

		// and test scheduling...
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(11, res.size());
	}

	@Test
	public void testAND() {
		EntityManager em = e1.ctx.getTransacEM();

		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		// Build the test chain
		Chain c1 = PlanBuilder.buildChain(a1, "chain on both nodes", "simple chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo a", "echoa", "a");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo b", "echob", "b");
		State s2 = PlanBuilder.buildState(c1, pg1, sc2);
		State s3 = PlanBuilder.buildStateAND(c1, pg1);

		c1.getStartState().connectTo(s1);
		c1.getStartState().connectTo(s2);
		s1.connectTo(s3);
		s2.connectTo(s3);
		s3.connectTo(c1.getEndState());

		// Save plan
		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Run the chain
		log.debug("**************************************************************************************");
		log.debug("****START OF CHAIN1*******************************************************************");
		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		} catch (JMSException e3) {
			Assert.fail(e3.getMessage());
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e3) {
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());
	}

	@Test
	public void testANDWithBarrier() {
		EntityManager em = e1.ctx.getTransacEM();

		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		// Build the test chains
		Calendar ca = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

		// First chain with the AND, with a State blocked by calendar
		Chain c1 = PlanBuilder.buildChain(a1, "chain on both nodes", "simple chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo a", "echoa", "a");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo b", "echob", "b");
		State s2 = PlanBuilder.buildState(c1, pg1, sc2);
		State s3 = PlanBuilder.buildStateAND(c1, pg1);

		s2.setCalendar(ca);
		s2.setCalendarShift(-1);

		c1.getStartState().connectTo(s1);
		c1.getStartState().connectTo(s2);
		s1.connectTo(s3);
		s2.connectTo(s3);
		s3.connectTo(c1.getEndState());

		// Second chain to advance calendar
		Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
		NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca);
		State s9 = PlanBuilder.buildState(c2, pg1, no);
		c2.getStartState().connectTo(s9);
		s9.connectTo(c2.getEndState());

		// Save to file & recycle
		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Shift the state by 1 so that it cannot start (well, shouldn't)
		log.debug("**************************************************************************************");
		log.debug("****SHIFT CALENDAR********************************************************************");
		try {
			SenderHelpers.sendCalendarPointerShift(1, s2, e1.ctx);
			Thread.sleep(500);
		} catch (Exception e4) {
			e4.printStackTrace();
			Assert.fail(e4.getMessage());
		}

		// Run the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER START OF CHAIN1*************************************************************");
		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
			Thread.sleep(2000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(2, res.size());

		TypedQuery<Event> q1 = e1.ctx.getTransacEM().createQuery("SELECT e FROM Event e", Event.class);
		Assert.assertEquals(2, q1.getResultList().size());

		// Run second chain - should unlock the first chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER START SECOND CHAIN**********************************************************");
		try {
			SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(4000); // Process events
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(8, res.size());
		Assert.assertEquals(0, q1.getResultList().size()); // events
	}
}
