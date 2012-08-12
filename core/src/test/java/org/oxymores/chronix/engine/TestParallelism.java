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
	PlaceGroup pg1, pg2, pg3, pg4;

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
		p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);
		p2 = PlanBuilder.buildPlace(a1, "second node", "second node", en2);
		p3 = PlanBuilder.buildPlace(a1, "hosted node by second node", "third node", en3);

		pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
		pg2 = PlanBuilder.buildPlaceGroup(a1, "second node", "second node", p2);
		pg3 = PlanBuilder.buildPlaceGroup(a1, "hosted node by second node", "third node", p3);
		pg4 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1, p2, p3);

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
		try {
			SenderHelpers.sendApplication(a1, en2, e1.ctx);
			Thread.sleep(500); // integrate the application, restart node...
			e2.waitForInitEnd();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		// Launch state
		try {
			SenderHelpers.runStateInsidePlanWithoutCalendarUpdating(c1.getStartState(), p1, e1.ctx);
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
		e1.stopEngine();
		e1.waitForStopEnd();
		e2.stopEngine();
		e2.waitForStopEnd();
		e3.stopEngine();
		e3.waitForStopEnd();
	}

}
