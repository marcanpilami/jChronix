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
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.demo.PlanBuilder;

public class TestChain {
	private static Logger log = Logger.getLogger(TestChain.class);

	private String db1;
	Application a1;
	ChronixEngine e1;
	ExecutionNode en1;
	Place p1;
	PlaceGroup pg1, pg2;

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

	@After
	public void cleanup() {
		log.debug("**************************************************************************************");
		log.debug("****END OF TEST***********************************************************************");
		if (e1 != null && e1.run) {
			e1.stopEngine();
			e1.waitForStopEnd();
		}
	}

	@Test
	public void testChainLaunch() throws Exception {
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chain
		Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 1\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp = PlanBuilder.buildState(p1, pg1, c1);

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
	public void testCompletePlan() throws Exception {
		log.debug("**************************************************************************************");
		log.debug("****CREATE PLAN***********************************************************************");

		EntityManager em = e1.ctx.getTransacEM();

		// First stupid chains
		Chain c1 = PlanBuilder.buildChain(a1, "simple chain 1", "chain1", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 1\"", "echo oo", "oo");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		Chain c2 = PlanBuilder.buildChain(a1, "simple chain 2", "chain2", pg1);
		State s2 = PlanBuilder.buildState(c2, pg1, sc1);
		c2.getStartState().connectTo(s2);
		s2.connectTo(c2.getEndState());

		Chain c3 = PlanBuilder.buildChain(a1, "simple chain 3", "chain3", pg1);
		State s3 = PlanBuilder.buildState(c3, pg1, sc1);
		c3.getStartState().connectTo(s3);
		s3.connectTo(c3.getEndState());

		Chain c4 = PlanBuilder.buildChain(a1, "simple chain 4", "chain4", pg1);
		State s4 = PlanBuilder.buildState(c4, pg1, sc1);
		c4.getStartState().connectTo(s4);
		s4.connectTo(c4.getEndState());

		// Plan that contains the other chains
		Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
		State sp1 = PlanBuilder.buildState(p1, pg1, c1);
		State sp2 = PlanBuilder.buildState(p1, pg1, c2);
		State sp3 = PlanBuilder.buildState(p1, pg1, c3);
		State sp4 = PlanBuilder.buildState(p1, pg1, c4);
		State spA = PlanBuilder.buildStateAND(p1, pg1);

		sp1.connectTo(sp2);
		sp1.connectTo(sp3);
		sp2.connectTo(spA);
		sp3.connectTo(spA);
		spA.connectTo(sp4);

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
		SenderHelpers.runStateInsidePlan(sp1, e1.ctx, em);
		Thread.sleep(8000); // Time to consume message

		// Tests
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(17, res.size());
		RunLog rl0 = res.get(0);

		Assert.assertEquals(rl0.activeNodeName, "simple chain 1");
	}
}
