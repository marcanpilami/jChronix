package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;
import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.demo.PlanBuilder;

public class TestHostedNode
{
	private static Logger log = Logger.getLogger(TestHostedNode.class);

	private String db1, db2;
	Application a1;
	ChronixEngine e1, e2;
	ExecutionNode en1, en2;
	Place p1, p2;
	PlaceGroup pg1, pg2, pg3;

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
	}

	@Before
	public void prepare() throws Exception
	{
		db1 = "C:\\TEMP\\db1";
		db2 = "C:\\TEMP\\db3";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1, "localhost:1789");
		e1.emptyDb();
		LogHelpers.clearAllTranscientElements(e1.ctx);

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Hosted node test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, "localhost", 1789);
		en1.setConsole(true);
		en2 = PlanBuilder.buildExecutionNode(a1, "localhost", 1804);
		en1.connectTo(en2, NodeConnectionMethod.RCTRL);

		// Logical network
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);
		p2 = PlanBuilder.buildPlace(a1, "hosted node", "hosted node", en2);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "hosted node", "hosted node", p2);
		pg3 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1, p2);

		// Chains and other stuff depend are created by the tests themselves

		// Save app in node 1
		try
		{
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		/************************************************
		 * Create an empty test configuration db for second node
		 ***********************************************/
		e2 = new ChronixEngine(db2, "localhost:1804", "TransacUnitXXX", "HistoryUnitXXX", true);
		e2.emptyDb();

		/************************************************
		 * Start the engines
		 ***********************************************/

		e1.start();
		e2.start();
		log.debug("Started - begin waiting");
		e1.waitForInitEnd();
		e2.waitForInitEnd();
		log.debug("Engines inits done");
	}

	@Test
	public void testSimpleChain()
	{
		EntityManager em = e1.ctx.getTransacEM();

		// Build a very simple chain
		Chain c1 = PlanBuilder.buildChain(a1, "chain fully on hosted node", "simple chain", pg2);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg2, sc1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

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

		// Run the chain
		try
		{
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(3, res.size());
	}

	@Test
	public void testMasterAndSlaveChain()
	{
		EntityManager em = e1.ctx.getTransacEM();

		// Build the test chain
		Chain c1 = PlanBuilder.buildChain(a1, "chain on both nodes", "simple chain", pg2);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo a", "echoa", "a");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);

		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo b", "echob", "b");
		State s2 = PlanBuilder.buildState(c1, pg2, sc2);

		ShellCommand sc3 = PlanBuilder.buildShellCommand(a1, "echo c", "echoc", "c");
		State s3 = PlanBuilder.buildState(c1, pg1, sc3);

		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s2.connectTo(s3);
		s3.connectTo(c1.getEndState());

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

		// Run the chain
		try
		{
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());
	}

	@Test
	public void testMixedConditionChain()
	{
		EntityManager em = e1.ctx.getTransacEM();

		// Build the test chain
		Chain c1 = PlanBuilder.buildChain(a1, "chain on both nodes", "simple chain", pg2);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo a", "echoa", "a");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);

		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo b", "echob", "b");
		State s2 = PlanBuilder.buildState(c1, pg2, sc2);
		State s3 = PlanBuilder.buildStateAND(c1, pg1);

		c1.getStartState().connectTo(s1);
		c1.getStartState().connectTo(s2);
		s1.connectTo(s3);
		s2.connectTo(s3);
		s3.connectTo(c1.getEndState());

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

		// Run the chain
		try
		{
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
		} catch (JMSException e3)
		{
			Assert.fail(e3.getMessage());
		}

		try
		{
			Thread.sleep(3000);
		} catch (InterruptedException e3)
		{
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(5, res.size());
	}

}
