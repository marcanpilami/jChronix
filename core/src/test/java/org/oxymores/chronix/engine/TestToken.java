package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestToken
{
	private static Logger log = Logger.getLogger(TestToken.class);

	private String db1;
	Application a1;
	ChronixEngine e1;
	ExecutionNode en1;
	Place p1, p2;
	PlaceGroup pg1, pg2;

	@Before
	public void prepare() throws Exception
	{
		db1 = "C:\\TEMP\\db1";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1, "localhost:1789");
		e1.emptyDb();
		LogHelpers.clearAllTranscientElements(e1.ctx);

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Single node test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, "localhost", 1789);
		en1.setConsole(true);

		// Logical network
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);
		p2 = PlanBuilder.buildPlace(a1, "second", "second node on master", en1);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1, p2);

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
		 * Start the engine
		 ***********************************************/

		e1.start();
		log.debug("Started - begin waiting");
		e1.waitForInitEnd();
		log.debug("Engines inits done");
	}

	@After
	public void cleanup()
	{
		log.debug("**************************************************************************************");
		log.debug("****END OF TEST***********************************************************************");
		if (e1 != null && e1.shouldRun())
		{
			e1.stopEngine();
			e1.waitForStopEnd();
		}
	}

	@Test
	public void testAlone() throws Exception
	{
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chain
		Token tk1 = PlanBuilder.buildToken(a1, "my token");

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 1\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		s1.addToken(tk1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp = PlanBuilder.buildState(p1, pg1, c1);

		// Save plan
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Start chain
		log.debug("**************************************************************************************");
		log.debug("****FIRST (PASSING) RUN***************************************************************");
		SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
		Thread.sleep(5000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(4, res.size());
		RunLog rl0 = res.get(0);
		RunLog rl3 = res.get(3);
		DateTime end0 = new DateTime(rl0.stoppedRunningAt);
		DateTime end3 = new DateTime(rl3.stoppedRunningAt);

		Assert.assertEquals(rl0.activeNodeName, "simple chain");
		Assert.assertTrue(end0.isAfter(end3) || end0.isEqual(end3));
	}

	@Test
	public void testAloneBlocking() throws Exception
	{
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chain
		Token tk1 = PlanBuilder.buildToken(a1, "my token", 0);

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 1\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		s1.addToken(tk1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp = PlanBuilder.buildState(p1, pg1, c1);

		// Save plan
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Start chain
		log.debug("**************************************************************************************");
		log.debug("****FIRST (PASSING) RUN***************************************************************");
		SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
		Thread.sleep(5000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(1, res.size());
	}

	@Test
	public void testTwoWithOne() throws Exception
	{
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chain
		Token tk1 = PlanBuilder.buildToken(a1, "my token");

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 2\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg2, sc1);
		s1.addToken(tk1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp = PlanBuilder.buildState(p1, pg1, c1);

		// Save plan
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Start chain
		log.debug("**************************************************************************************");
		log.debug("****FIRST (PASSING) RUN***************************************************************");
		SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
		Thread.sleep(5000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());
		RunLog rl2 = res.get(2);
		RunLog rl3 = res.get(3);
		DateTime end2 = new DateTime(rl2.stoppedRunningAt);
		DateTime start2 = new DateTime(rl3.beganRunningAt);

		Assert.assertTrue(start2.isAfter(end2));
	}

	@Test
	public void testTwoWithOnePerPlace() throws Exception
	{
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chain
		Token tk1 = PlanBuilder.buildToken(a1, "my token", 1, true);

		Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 2\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg2, sc1);
		s1.addToken(tk1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp = PlanBuilder.buildState(p1, pg1, c1);

		// Save plan
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Start chain
		log.debug("**************************************************************************************");
		log.debug("****FIRST (PASSING) RUN***************************************************************");
		SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
		Thread.sleep(5000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());
		RunLog rl2 = res.get(2);
		RunLog rl3 = res.get(3);
		DateTime end3 = new DateTime(rl3.stoppedRunningAt);
		DateTime start3 = new DateTime(rl3.beganRunningAt);
		DateTime end2 = new DateTime(rl2.stoppedRunningAt);
		DateTime start2 = new DateTime(rl2.beganRunningAt);

		Assert.assertTrue(end2.isBefore(start3) || end3.isBefore(start2));
	}
}
