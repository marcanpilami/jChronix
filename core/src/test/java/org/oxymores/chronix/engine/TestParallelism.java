package org.oxymores.chronix.engine;

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
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

public class TestParallelism {
	private static Logger log = Logger.getLogger(TestParallelism.class);

	private String db1, db2, db3;
	Application a1;
	ChronixEngine e1, e2, e3;
	ExecutionNode en1, en2, en3;
	Place p1, p2, p3;
	Place h11, h12, h21, h22, h31, h32;
	PlaceGroup pg1, pg2, pg3, pg4;
	PlaceGroup groupOneEach, groupAllNodes, groupnode1, groupnode2, groupnode3;

	public void prepare() throws Exception {
		if (a1 != null)
			return;

		db1 = "C:\\TEMP\\db1";
		db2 = "C:\\TEMP\\db2";
		db3 = "C:\\TEMP\\db3";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1);
		e1.emptyDb();
		e1.ctx.createNewConfigFile();
		LogHelpers.clearAllTranscientElements(e1.ctx);

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Multinode test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, 1789);
		en1.setConsole(true);
		en2 = PlanBuilder.buildExecutionNode(a1, 1400);
		en3 = PlanBuilder.buildExecutionNode(a1, 1804);
		en1.connectTo(en2, NodeConnectionMethod.TCP);
		en2.connectTo(en3, NodeConnectionMethod.RCTRL);

		// Logical network
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node reference place", en1);
		p2 = PlanBuilder.buildPlace(a1, "second node", "second node reference place", en2);
		p3 = PlanBuilder.buildPlace(a1, "hosted node by second node reference place", "third node", en3);

		h11 = PlanBuilder.buildPlace(a1, "P11", "master node 1", en1);
		h12 = PlanBuilder.buildPlace(a1, "P12", "master node 2", en1);
		h21 = PlanBuilder.buildPlace(a1, "P21", "second node 1", en2);
		h22 = PlanBuilder.buildPlace(a1, "P22", "second node 2", en2);
		h31 = PlanBuilder.buildPlace(a1, "P31", "hosted node by second node 1", en3);
		h32 = PlanBuilder.buildPlace(a1, "P32", "hosted node by second node 2", en3);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node reference group", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "second node reference group", "second node", p2);
		pg3 = PlanBuilder.buildPlaceGroup(a1, "hosted node by second node reference group", "third node", p3);
		pg4 = PlanBuilder.buildPlaceGroup(a1, "all reference places", "all nodes", p1, p2, p3);

		groupOneEach = PlanBuilder.buildPlaceGroup(a1, "one place on each node", "", h11, h21, h31);
		groupAllNodes = PlanBuilder.buildPlaceGroup(a1, "all nodes places", "all nodes", h11, h12, h21, h22, h31, h32);
		groupnode1 = PlanBuilder.buildPlaceGroup(a1, "all node 1 places", "", h11, h12);
		groupnode2 = PlanBuilder.buildPlaceGroup(a1, "all node 2 places", "", h21, h22);
		groupnode3 = PlanBuilder.buildPlaceGroup(a1, "all node 3 places", "", h31, h32);

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
		 * Create an empty test configuration db for second node
		 ***********************************************/
		e2 = new ChronixEngine(db2);
		e2.emptyDb();
		e2.ctx.createNewConfigFile(1400, "TransacUnit2", "HistoryUnit2");
		LogHelpers.clearAllTranscientElements(e2.ctx);

		/************************************************
		 * Create an empty test configuration db for third node
		 ***********************************************/
		e3 = new ChronixEngine(db3, true);
		e3.emptyDb();
		e3.ctx.createNewConfigFile(1804, "TransacUnitXXX", "HistoryUnitXXX");

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

	@Test
	public void testEnvVars() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 1****************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		String nl = System.getProperty("line.separator");
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build a very simple chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");

		Chain c1 = PlanBuilder.buildChain(a1, "empty chain", "empty chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "set", "Display envt", "Will display all env vars");
		State s1 = PlanBuilder.buildState(c1, pg1, sc1);
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo $env:CHR_JOBNAME\"", "Display jobname",
				"Will display one env vars");
		State s2 = PlanBuilder.buildState(c1, pg1, sc2);
		ShellCommand sc3 = PlanBuilder.buildShellCommand(a1, "echo set MARSU=12 54 pohfgh)'", "Set a var",
				"Will set a new env vars that should be propagated");
		State s3 = PlanBuilder.buildState(c1, pg1, sc3);
		ShellCommand sc4 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo $env:MARSU\"", "Display MARSU",
				"Will display the MARSU env var");
		State s4 = PlanBuilder.buildState(c1, pg1, sc4);
		State s5 = PlanBuilder.buildState(c1, pg3, sc4);
		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s2.connectTo(s3);
		s3.connectTo(s4);
		s4.connectTo(s5);
		s5.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");

		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch state
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(2000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished OK
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(7, res.size());
		for (RunLog rl : res) {
			log.info(rl.shortLog);
		}

		// Test auto variable
		Assert.assertEquals(nl + "Display jobname", res.get(2).shortLog);

		// Test propagated variable on the local node
		Assert.assertEquals(nl + "12 54 pohfgh)'", res.get(4).shortLog);

		// Test propagated variable on the remotely hosted node
		Assert.assertEquals(nl + "12 54 pohfgh)'", res.get(5).shortLog);

		// Close
		log.debug("**************************************************************************************");
		log.debug("****TEST END**************************************************************************");

		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

	@Test
	public void testTestJob() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("****TEST 2 - preparatory *************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build a very simple chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");
		Chain c1 = PlanBuilder.buildChain(a1, "empty chain", "empty chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1,
				"powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
				"Will fail with return code 19 on P2, be OK on other places");
		State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1);
		c1.getStartState().connectTo(s1);
		s1.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch chain
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(5000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished nominally
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(7, res.size());
		RunLog failedLog = null;
		for (RunLog rl : res) {
			log.info(rl.shortLog);
			if (rl.placeName.startsWith("P21"))
				failedLog = rl;
		}

		// Restart failed job
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO RESTART*******************************************************");
		try {
			SenderHelpers.sendOrderRestartAfterFailure(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(7, res.size());

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(8, res.size());

		// Close
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

	@Test
	public void testCaseP1() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**** TEST 3 - case P1 ****************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build a very simple chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1,
				"powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
				"Will fail with return code 19 on P2, be OK on other places");
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo 'job done'\"", "Always OK", "Always OK");

		Chain c1 = PlanBuilder.buildChain(a1, "chain P1", "chain P1", pg1);
		State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
		State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc2, true);
		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s2.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch chain
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(5000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished nominally
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(12, res.size());
		RunLog failedLog = null;
		for (RunLog rl : res) {
			if (rl.placeName.startsWith("P21"))
				failedLog = rl;
		}

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(14, res.size());

		// Close
		log.debug("**************************************************************************************");
		log.debug("**** TEST END ************************************************************************");
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

	@Test
	public void testCaseP2() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**** TEST 4 - case P2 ****************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build the test chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1,
				"powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
				"Will fail with return code 19 on P2, be OK on other places");
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo 'job done'\"", "Always OK", "Always OK");

		Chain c1 = PlanBuilder.buildChain(a1, "chain P1", "chain P1", pg1);
		State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc2, true);
		State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc2, true);
		State s3 = PlanBuilder.buildState(c1, groupnode2, sc1, true);
		State s4 = PlanBuilder.buildStateAND(c1, groupAllNodes);
		c1.getStartState().connectTo(s1);
		c1.getStartState().connectTo(s3);
		s1.connectTo(s4);
		s3.connectTo(s4);
		s4.connectTo(s2);
		s2.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch chain
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(5000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished nominally
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(9, res.size());
		RunLog failedLog = null;
		for (RunLog rl : res) {
			if (rl.placeName.startsWith("P21"))
				failedLog = rl;
		}

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(22, res.size());

		// Close
		log.debug("**************************************************************************************");
		log.debug("**** TEST END ************************************************************************");
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

	@Test
	public void testCaseP5() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**** TEST 5 - case P5 ****************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build the test chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1,
				"powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
				"Will fail with return code 19 on P2, be OK on other places");

		Chain c1 = PlanBuilder.buildChain(a1, "chain P1", "chain P1", pg1);
		State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1);
		State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
		State s3 = PlanBuilder.buildState(c1, groupAllNodes, sc1);

		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s2.connectTo(s3);
		s3.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch chain
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(5000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished nominally
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(7, res.size());
		RunLog failedLog = null;
		for (RunLog rl : res) {
			log.debug(rl.stateId);
			if (rl.placeName.startsWith("P21") && rl.stateId.equals(s1.getId().toString()))
				failedLog = rl;
		}

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			log.error("oups", e);
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(13, res.size());
		failedLog = null;
		for (RunLog rl : res) {
			if (rl.placeName.startsWith("P21") && rl.stateId.equals(s2.getId().toString()))
				failedLog = rl;
		}

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON EPISODE 2*********************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(19, res.size());
		failedLog = null;
		for (RunLog rl : res) {
			if (rl.placeName.startsWith("P21") && rl.stateId.equals(s3.getId().toString()))
				failedLog = rl;
		}

		// Ask the failed job to free the rest of the chain
		log.debug("**************************************************************************************");
		log.debug("****ORDER FAILED JOB TO ABANDON EPISODE 3*********************************************");
		try {
			SenderHelpers.sendOrderForceOk(failedLog.id, en2, e1.ctx);
			Thread.sleep(2000);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(20, res.size());

		// Close
		log.debug("**************************************************************************************");
		log.debug("**** TEST END ************************************************************************");
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

	@Test
	public void testCaseP1Prime() {
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**** TEST 6 - case P1' ***************************************************************");
		log.debug("**************************************************************************************");
		log.debug("**************************************************************************************");

		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build the test chain
		log.debug("**************************************************************************************");
		log.debug("****CREATE CHAIN**********************************************************************");
		ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo 'job done'\"", "A - Always OK", "Always OK");
		ShellCommand sc2 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"echo 'job done'\"", "B - Always OK", "Always OK");

		Chain c1 = PlanBuilder.buildChain(a1, "chain P1", "chain P1", pg1);
		State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
		State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
		State s3 = PlanBuilder.buildState(c1, groupAllNodes, sc2, true);
		State s4 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
		State s5 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);

		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s2.connectTo(s3);
		s3.connectTo(s4);
		s4.connectTo(s5);
		s5.connectTo(c1.getEndState());

		try {
			e1.ctx.saveApplication(a1);
			e1.ctx.setWorkingAsCurrent(a1);
			e1.queueReloadConfiguration();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

		// Send the chain to node 2
		log.debug("**************************************************************************************");
		log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
			e1.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch chain
		log.debug("**************************************************************************************");
		log.debug("****START CHAIN***********************************************************************");

		try {
			SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, e1.ctx.getTransacEM());
			Thread.sleep(5000);
		} catch (Exception e3) {
			Assert.fail(e3.getMessage());
		}

		// Test finished nominally
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(32, res.size());

		// Close
		log.debug("**************************************************************************************");
		log.debug("**** TEST END ************************************************************************");
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}
}
