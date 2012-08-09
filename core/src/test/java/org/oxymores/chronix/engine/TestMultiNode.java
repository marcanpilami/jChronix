package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;

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

public class TestMultiNode {
	private static Logger log = Logger.getLogger(TestMultiNode.class);

	private String db1, db2;
	Application a1;
	ChronixEngine e1, e2;
	ExecutionNode en1, en2, en3;
	Place p1, p2, p3;
	PlaceGroup pg1, pg2, pg3, pg4;

	public void prepare() throws Exception {
		if (a1 != null)
			return;

		db1 = "C:\\TEMP\\db1";
		db2 = "C:\\TEMP\\db2";

		/************************************************
		 * Create a test configuration db
		 ***********************************************/

		e1 = new ChronixEngine(db1);
		e1.emptyDb();
		e1.ctx.createNewConfigFile();

		// Create a test application and save it inside context
		a1 = PlanBuilder.buildApplication("Multinode test", "test");

		// Physical network
		en1 = PlanBuilder.buildExecutionNode(a1, 1789);
		en1.setConsole(true);
		en2 = PlanBuilder.buildExecutionNode(a1, 1400);
		en3 = PlanBuilder.buildExecutionNode(a1, 0);
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

	public void sendA(Application a) throws JMSException {
		log.debug("**************************************************************************************");
		SenderHelpers.sendApplication(a, en2, e1.ctx);
	}

	@Test
	public void testSend() {
		try {
			prepare();
			sendA(e1.ctx.applicationsByName.get("Multinode test"));
			Thread.sleep(5000); // integrate the application, restart node...
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}

		Application a2 = null;
		try {
			a2 = e2.ctx.applicationsByName.get("Multinode test");
		} catch (Exception e) {
			Assert.fail();
		}

		log.debug(a2.getPlaces());
		Assert.assertEquals(3, a2.getPlaces().values().size());
		Assert.assertEquals(0, a2.getChains().size());

		// Close
		try {
			e1.stopEngine();
			Thread.sleep(500); // Since both stop at the same moment, could
								// create harmless errors without ordering stops
			e2.stopEngine();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	@Test
	public void testSimpleChain() {
		// Prepare
		try {
			prepare();
		} catch (Exception e3) {
			e3.printStackTrace();
			Assert.fail(e3.getMessage());
		}

		// Build a very simple chain
		Chain c1 = PlanBuilder.buildChain(a1, "empty chain", "empty chain", pg1);
		ShellCommand sc1 = PlanBuilder.buildNewActiveShell(a1, "echo oo", "echo oo", "oo");
		State s1 = PlanBuilder.buildNewState(c1, pg2, sc1);
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
		try {
			prepare();
			sendA(a1);
			Thread.sleep(10000); // integrate the application, restart node...
		} catch (Exception e) {
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
		try {
			SenderHelpers.runStateInsidePlanWithoutCalendarUpdating(c1.getStartState(), p1, e1.ctx);
		} catch (JMSException e3) {
			Assert.fail(e3.getMessage());
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e3) {
		}
		List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
		Assert.assertEquals(3, res.size());

		// Close
		try {
			e1.stopEngine();
			Thread.sleep(500); // Since both stop at the same moment, could
								// create harmless errors without ordering stops
			e2.stopEngine();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}
}
