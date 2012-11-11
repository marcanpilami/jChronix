package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;
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
import org.oxymores.chronix.core.NodeConnectionMethod;
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

public class TestMultiNode
{
	private static Logger log = Logger.getLogger(TestMultiNode.class);

	private String db1, db2, db3;
	Application a1;
	ChronixEngine e1, e2, e3;
	ExecutionNode en1, en2, en3;
	Place p1, p2, p3;
	PlaceGroup pg1, pg2, pg3, pg4;

	@After
	public void cleanup()
	{
		log.debug("**************************************************************************************");
		log.debug("****END OF TEST***********************************************************************");
		if (e1 != null && e1.run)
		{
			e1.stopEngine();
			e1.waitForStopEnd();
		}
		if (e2 != null && e2.run)
		{
			e2.stopEngine();
			e2.waitForStopEnd();
		}
		if (e3 != null && e3.run)
		{
			e3.stopEngine();
			e3.waitForStopEnd();
		}
	}

	@Before
	public void prepare() throws Exception
	{
		if (a1 != null)
			return;

		db1 = "C:\\TEMP\\db1";
		db2 = "C:\\TEMP\\db2";
		db3 = "C:\\TEMP\\db3";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1, "localhost:1789");
		e1.emptyDb();
		LogHelpers.clearAllTranscientElements(e1.ctx);

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Multinode test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, "localhost", 1789);
		en1.setConsole(true);
		en2 = PlanBuilder.buildExecutionNode(a1, "localhost", 1400);
		en3 = PlanBuilder.buildExecutionNode(a1, "localhost", 1804);
		en1.connectTo(en2, NodeConnectionMethod.TCP);
		en2.connectTo(en3, NodeConnectionMethod.RCTRL);

		// Logical network
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);
		p2 = PlanBuilder.buildPlace(a1, "second node", "second node", en2);
		p3 = PlanBuilder.buildPlace(a1, "hosted node by second node", "third node", en3);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "second node", "second node", p2);
		pg3 = PlanBuilder.buildPlaceGroup(a1, "hosted node by second node", "third node", p3);
		pg4 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1, p2, p3);

		// Chains and other stuff depends on the test

		// Save app in node 1
		try
		{
			e1.ctx.saveApplication(a1);
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		try
		{
			e1.ctx.setWorkingAsCurrent(a1);
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		/************************************************
		 * Create an empty test configuration db for second node
		 ***********************************************/
		e2 = new ChronixEngine(db2, "localhost:1400", "TransacUnit2", "HistoryUnit2");
		e2.emptyDb();
		LogHelpers.clearAllTranscientElements(e2.ctx);

		/************************************************
		 * Create an empty test configuration db for third node
		 ***********************************************/
		e3 = new ChronixEngine(db3, "localhost:1804", "TransacUnitXXX", "HistoryUnitXXX", true);
		e3.emptyDb();

		/************************************************
		 * Start the engines
		 ***********************************************/

		log.debug("Starting first node");
		e1.start();
		log.debug("Starting second node");
		e2.start();
		log.debug("Starting third node");
		e3.start();
		log.debug("Started - begin waiting");
		e1.waitForInitEnd();
		log.debug("First node has started");
		e2.waitForInitEnd();
		log.debug("Second node has started");
		e3.waitForInitEnd();
		log.debug("All engines inits done");
	}

	public void sendA(Application a) throws JMSException
	{
		log.debug("**************************************************************************************");
		SenderHelpers.sendApplication(a, en2, e1.ctx);
	}

	@Test
	public void testSend()
	{
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 1****************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		try
		{
			sendA(e1.ctx.applicationsByName.get("Multinode test"));
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}

		Application a2 = null;
		try
		{
			a2 = e2.ctx.applicationsByName.get("Multinode test");
		} catch (Exception e)
		{
			Assert.fail();
		}

		log.debug(a2.getPlaces());
		Assert.assertEquals(3, a2.getPlaces().values().size());
		Assert.assertEquals(0, a2.getChains().size());
	}

	@Test
	public void testSimpleChain()
	{
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 2****************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Build a very simple chain
		Chain c1 = PlanBuilder.buildChain(a1, "empty chain", "empty chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg2, sc1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		try
		{
			prepare();
			sendA(a1);
			Thread.sleep(500); // integrate the application, restart node...
			e1.waitForInitEnd();
			e2.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}

		// Test reception is OK
		Application a2 = e2.ctx.applicationsByName.get("Multinode test");
		if (a2 == null)
			Assert.fail("No application in remote context after reception");

		log.debug(a2.getPlaces());
		Assert.assertEquals(3, a2.getPlaces().values().size());
		Assert.assertEquals(1, a2.getChains().size());

		// Run the chain
		try
		{
			SenderHelpers.runStateInsidePlanWithoutCalendarUpdating(c1.getStartState(), p1, e1.ctx);
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(2000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(3, res.size());
	}

	@Test
	public void testCalendarTransmission()
	{
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 3****************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");

		// Build the test chains //////////////////////
		Calendar ca = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain using calendar", "chain2", pg2);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg2, sc1);
		s1.setCalendar(ca);
		s1.setCalendarShift(-1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
		NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca);
		State s2 = PlanBuilder.buildState(c2, pg1, no);
		c2.getStartState().connectTo(s2);
		s2.connectTo(c2.getEndState());
		// //////////////////////////////////////////////

		log.debug("**************************************************************************************");
		log.debug("****SAVE CHAIN************************************************************************");
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN************************************************************************");
		// Send the chain to node 2
		try
		{
			prepare();
			sendA(a1);
			e1.waitForInitEnd();
			e2.waitForInitEnd();
			log.debug("Application integration should be over by now...");
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}

		// Test reception is OK
		Application a2 = e2.ctx.applicationsByName.get("Multinode test");
		if (a2 == null)
			Assert.fail("No application in remote context after reception");
		Assert.assertEquals(3, a2.getPlaces().values().size());

		TypedQuery<CalendarPointer> q2 = e1.ctx.getTransacEM().createQuery("SELECT cp FROM CalendarPointer cp", CalendarPointer.class);
		Assert.assertEquals(2, q2.getResultList().size());

		log.debug("**************************************************************************************");
		log.debug("****SHIFT CALENDAR********************************************************************");
		// Shift the state by 1 so that it cannot start (well, shouldn't)
		try
		{
			SenderHelpers.sendCalendarPointerShift(1, s1, e1.ctx);
			Thread.sleep(500);
		} catch (Exception e4)
		{
			e4.printStackTrace();
			Assert.fail(e4.getMessage());
		}
		Assert.assertEquals(2, q2.getResultList().size());
		TypedQuery<CalendarPointer> q3 = e2.ctx.getTransacEM().createQuery("SELECT cp FROM CalendarPointer cp", CalendarPointer.class);
		Assert.assertEquals(2, q3.getResultList().size());

		// Run the first chain - should be blocked after starting State
		log.debug("**************************************************************************************");
		log.debug("****ORDER START FIRST CHAIN***********************************************************");
		try
		{
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(2000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(1, res.size());

		// Run second chain - should unlock the first chain on the other node
		log.debug("**************************************************************************************");
		log.debug("****ORDER START SECOND CHAIN**********************************************************");
		try
		{
			SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx, e1.ctx.getTransacEM());
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(2000); // Process events
		} catch (InterruptedException e3)
		{
		}

		// Have all jobs run?
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(6, res.size());

		// Has purge worked?
		TypedQuery<Event> q4 = e2.ctx.getTransacEM().createQuery("SELECT e FROM Event e", Event.class);
		Assert.assertEquals(0, q4.getResultList().size());

		TypedQuery<Event> q5 = e1.ctx.getTransacEM().createQuery("SELECT e FROM Event e", Event.class);
		Assert.assertEquals(0, q5.getResultList().size());
	}

	@Test
	public void testRemoteHostedAgent()
	{
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 4****************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");

		// Build the test chains //////////////////////
		Calendar ca = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

		Chain c1 = PlanBuilder.buildChain(a1, "AND plus CALENDAR", "chain2", pg2);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo aa", "echo aa", "aa");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo bb", "echo bb", "bb");
		State s2 = PlanBuilder.buildState(c1, pg2, sc2);
		ShellCommand sc3 = PlanBuilder.buildShellCommand(a1, "echo cc", "echo cc", "cc");
		State s3 = PlanBuilder.buildState(c1, pg3, sc3);
		ShellCommand sc4 = PlanBuilder.buildShellCommand(a1, "echo dd", "echo dd", "dd");
		State s4 = PlanBuilder.buildState(c1, pg1, sc4);
		State s5 = PlanBuilder.buildStateAND(c1, pg3);

		s4.setCalendar(ca);
		s4.setCalendarShift(-1);

		c1.getStartState().connectTo(s1);
		c1.getStartState().connectTo(s2);
		c1.getStartState().connectTo(s3);

		s1.connectTo(s5);
		s2.connectTo(s5);
		s3.connectTo(s5);

		s5.connectTo(s4);
		s4.connectTo(c1.getEndState());

		Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
		NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca);
		State s9 = PlanBuilder.buildState(c2, pg1, no);
		c2.getStartState().connectTo(s9);
		s9.connectTo(c2.getEndState());
		// //////////////////////////////////////////////

		log.debug("**************************************************************************************");
		log.debug("****SAVE CHAIN************************************************************************");
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN************************************************************************");
		// Send the chain to node 2
		try
		{
			prepare();
			sendA(a1);
			e1.waitForInitEnd();
			e2.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail();
		}

		// Test reception is OK
		Application a2 = e2.ctx.applicationsByName.get("Multinode test");
		if (a2 == null)
			Assert.fail("No application in remote context after reception");
		Assert.assertEquals(3, a2.getPlaces().values().size());

		TypedQuery<CalendarPointer> q2 = e1.ctx.getTransacEM().createQuery("SELECT cp FROM CalendarPointer cp", CalendarPointer.class);
		Assert.assertEquals(2, q2.getResultList().size());

		log.debug("**************************************************************************************");
		log.debug("****SHIFT CALENDAR********************************************************************");
		// Shift the state by 1 so that it cannot start (well, shouldn't)
		try
		{
			SenderHelpers.sendCalendarPointerShift(1, s4, e1.ctx);
			Thread.sleep(500);
		} catch (Exception e4)
		{
			e4.printStackTrace();
			Assert.fail(e4.getMessage());
		}
		Assert.assertEquals(2, q2.getResultList().size());
		TypedQuery<CalendarPointer> q3 = e2.ctx.getTransacEM().createQuery("SELECT cp FROM CalendarPointer cp", CalendarPointer.class);
		Assert.assertEquals(2, q3.getResultList().size());

		// Run the first chain - should be blocked after the AND
		log.debug("**************************************************************************************");
		log.debug("****ORDER START FIRST CHAIN***********************************************************");
		try
		{
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(2000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());

		// Run second chain - should unlock the first chain on the other node
		log.debug("**************************************************************************************");
		log.debug("****ORDER START SECOND CHAIN**********************************************************");
		try
		{
			SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx, e1.ctx.getTransacEM());
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(2000); // Process events
		} catch (InterruptedException e3)
		{
		}

		// Have all jobs run?
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(10, res.size());

		// Has purge worked?
		TypedQuery<Event> q4 = e2.ctx.getTransacEM().createQuery("SELECT e FROM Event e", Event.class);
		Assert.assertEquals(0, q4.getResultList().size());

		TypedQuery<Event> q5 = e1.ctx.getTransacEM().createQuery("SELECT e FROM Event e", Event.class);
		Assert.assertEquals(0, q5.getResultList().size());
	}
}
