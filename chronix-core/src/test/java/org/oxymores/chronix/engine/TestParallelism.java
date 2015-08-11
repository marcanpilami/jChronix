package org.oxymores.chronix.engine;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestParallelism extends TestBase
{
    Application a;
    ChronixEngine e1, e2, e3;
    ExecutionNode en1, en2, en3;
    Place p1, p2, p3;
    Place h11, h12, h21, h22, h31, h32;
    PlaceGroup pg1, pg2, pg3, pg4;
    PlaceGroup groupOneEach, groupAllNodes, groupnode1, groupnode2, groupnode3;

    @Before
    public void before() throws Exception
    {
        a = PlanBuilder.buildApplication("// test", "test");

        e1 = addEngine(db1, "e1");
        e2 = addEngine(db2, "e2");
        e3 = addRunner(db3, "e3", "localhost", 1804);

        // Create a test application and save it inside context
        a = PlanBuilder.buildApplication("Multinode test", "test");

        // Physical network
        n = new Network();
        en1 = PlanBuilder.buildExecutionNode(n, "e1", "localhost", 1789);
        en2 = PlanBuilder.buildExecutionNode(n, "e2", "localhost", 1400);
        en3 = PlanBuilder.buildExecutionNode(n, "e3", "localhost", 1804);
        en1.setConsole(true);
        en1.connectTo(en2, NodeConnectionMethod.TCP);
        en2.connectTo(en3, NodeConnectionMethod.RCTRL);

        // Logical network
        p1 = PlanBuilder.buildPlace(n, "master node", en1);
        p2 = PlanBuilder.buildPlace(n, "second node", en2);
        p3 = PlanBuilder.buildPlace(n, "hosted node by second node reference place", en3);

        h11 = PlanBuilder.buildPlace(n, "P11", en1);
        h12 = PlanBuilder.buildPlace(n, "P12", en1);
        h21 = PlanBuilder.buildPlace(n, "P21", en2);
        h22 = PlanBuilder.buildPlace(n, "P22", en2);
        h31 = PlanBuilder.buildPlace(n, "P31", en3);
        h32 = PlanBuilder.buildPlace(n, "P32", en3);

        pg1 = PlanBuilder.buildPlaceGroup(a, "master node reference group", "master node", p1);
        pg2 = PlanBuilder.buildPlaceGroup(a, "second node reference group", "second node", p2);
        pg3 = PlanBuilder.buildPlaceGroup(a, "hosted node by second node reference group", "third node", p3);
        pg4 = PlanBuilder.buildPlaceGroup(a, "all reference places", "all nodes", p1, p2, p3);

        groupOneEach = PlanBuilder.buildPlaceGroup(a, "one place on each node", "", h11, h21, h31);
        groupAllNodes = PlanBuilder.buildPlaceGroup(a, "all nodes places", "all places", h11, h12, h21, h22, h31, h32);
        groupnode1 = PlanBuilder.buildPlaceGroup(a, "all node 1 places", "", h11, h12);
        groupnode2 = PlanBuilder.buildPlaceGroup(a, "all node 2 places", "", h21, h22);
        groupnode3 = PlanBuilder.buildPlaceGroup(a, "all node 3 places", "", h31, h32);

        storeNetwork(db1, n);
        storeNetwork(db2, n);
    }

    @Test
    public void testEnvVars()
    {
        // Prepare
        String nl = System.getProperty("line.separator");

        // Build a very simple chain
        Chain c1 = PlanBuilder.buildChain(a, "empty chain", "empty chain", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "set", "Display envt", "Will display all env vars");
        State s1 = PlanBuilder.buildState(c1, pg1, sc1);
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo $env:CHR_JOBNAME\"", "Display jobname", "Will display one env vars");
        State s2 = PlanBuilder.buildState(c1, pg1, sc2);
        ShellCommand sc3 = PlanBuilder.buildShellCommand(a, "echo set MARSU=12 54 pohfgh)'", "Set a var", "Will set a new env vars that should be propagated");
        State s3 = PlanBuilder.buildState(c1, pg1, sc3);
        ShellCommand sc4 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo $env:MARSU\"", "Display MARSU", "Will display the MARSU env var");
        State s4 = PlanBuilder.buildState(c1, pg1, sc4);
        State s5 = PlanBuilder.buildState(c1, pg3, sc4);
        c1.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(s3);
        s3.connectTo(s4);
        s4.connectTo(s5);
        s5.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch state
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Test finished OK
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 7);
        Assert.assertEquals(7, res.size());
        for (RunLog rl : res)
        {
            log.info(rl.getShortLog());
        }

        // Test auto variable
        Assert.assertEquals("Display jobname", res.get(2).getShortLog());

        // Test propagated variable on the local node
        Assert.assertEquals("12 54 pohfgh)'", res.get(4).getShortLog());

        // Test propagated variable on the remotely hosted node
        Assert.assertEquals("12 54 pohfgh)'", res.get(5).getShortLog());
    }

    @Test
    public void testTestJob()
    {
        // Build a very simple chain
        Chain c1 = PlanBuilder.buildChain(a, "empty chain", "empty chain", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
                "Will fail with return code 19 on P21, be OK on other places");
        State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch chain
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 7);
        Assert.assertEquals(7, res.size());
        RunLog failedLog = null;
        for (RunLog rl : res)
        {
            log.info(rl.getShortLog());
            if (rl.getPlaceName().startsWith("P21"))
            {
                failedLog = rl;
            }
        }
        if (failedLog == null)
        {
            Assert.fail("there was no failed job");
        }
        LogHelpers.displayAllHistory(e1.ctx);

        // Restart failed job
        log.debug("****ORDER FAILED JOB TO RESTART*******************************************************");
        try
        {
            SenderHelpers.sendOrderRestartAfterFailure(failedLog.getId(), en2, e1.ctx);
            Thread.sleep(2000);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(7, res.size());

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
            Thread.sleep(2000);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 8);
        Assert.assertEquals(8, res.size());
    }

    @Test
    public void testCaseP1()
    {
        // Build a very simple chain
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
                "Will fail with return code 19 on P2, be OK on other places");
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo 'job done'\"", "Always OK", "Always OK");

        Chain c1 = PlanBuilder.buildChain(a, "chain P1", "chain P1", pg1);
        State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
        State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc2, true);
        c1.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch chain
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Test finished nominally
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 12);
        Assert.assertEquals(12, res.size());
        RunLog failedLog = null;
        for (RunLog rl : res)
        {
            if (rl.getPlaceName().startsWith("P21"))
            {
                failedLog = rl;
            }
        }

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 14);
        Assert.assertEquals(14, res.size());
    }

    @Test
    public void testCaseP2()
    {
        // Build the test chain
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
                "Will fail with return code 19 on P2, be OK on other places");
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo 'job done'\"", "Always OK", "Always OK");

        Chain c1 = PlanBuilder.buildChain(a, "chain P1", "chain P1", pg1);
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

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch chain
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Test finished nominally
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 9);
        Assert.assertEquals(9, res.size());
        RunLog failedLog = null;
        for (RunLog rl : res)
        {
            if (rl.getPlaceName().startsWith("P21"))
            {
                failedLog = rl;
            }
        }
        Assert.assertNotNull(failedLog);

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 22);
        Assert.assertEquals(22, res.size());
    }

    @Test
    public void testCaseP5()
    {
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"if ($env:CHR_PLACENAME -eq 'P21') { exit 19 } else {echo 'houba'; exit 0}\"", "Fail on P21",
                "Will fail with return code 19 on P2, be OK on other places");
        Chain c1 = PlanBuilder.buildChain(a, "chain P1", "chain P1", pg1);
        State s1 = PlanBuilder.buildState(c1, groupAllNodes, sc1);
        State s2 = PlanBuilder.buildState(c1, groupAllNodes, sc1, true);
        State s3 = PlanBuilder.buildState(c1, groupAllNodes, sc1);

        c1.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch chain
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Test finished nominally
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 7);
        Assert.assertEquals(7, res.size());
        RunLog failedLog = null;
        for (RunLog rl : res)
        {
            log.debug(rl.getStateId());
            if (rl.getPlaceName().startsWith("P21") && rl.getStateId().equals(s1.getId()))
            {
                failedLog = rl;
            }
        }
        Assert.assertNotNull(failedLog);

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON*******************************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
        }
        catch (Exception e)
        {
            log.error("oups", e);
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 13);
        Assert.assertEquals(13, res.size());
        failedLog = null;
        for (RunLog rl : res)
        {
            if (rl.getPlaceName().startsWith("P21") && rl.getStateId().equals(s2.getId()))
            {
                failedLog = rl;
            }
        }
        Assert.assertNotNull(failedLog);

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON EPISODE 2*********************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 19);
        Assert.assertEquals(19, res.size());
        failedLog = null;
        for (RunLog rl : res)
        {
            if (rl.getPlaceName().startsWith("P21") && rl.getStateId().equals(s3.getId()))
            {
                failedLog = rl;
            }
        }
        Assert.assertNotNull(failedLog);

        // Ask the failed job to free the rest of the chain
        log.debug("****ORDER FAILED JOB TO ABANDON EPISODE 3*********************************************");
        try
        {
            SenderHelpers.sendOrderForceOk(failedLog.getId(), en2, e1.ctx);
        }
        catch (Exception e)
        {
            Assert.fail(e.getMessage());
        }
        res = LogHelpers.waitForHistoryCount(e1.ctx, 20);
        Assert.assertEquals(20, res.size());
    }

    @Test
    public void testCaseP1Prime()
    {
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo 'job done'\"", "A - Always OK", "Always OK");
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "powershell.exe -Command \"echo 'job done'\"", "B - Always OK", "Always OK");

        Chain c1 = PlanBuilder.buildChain(a, "chain P1", "chain P1", pg1);
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

        addApplicationToDb(db1, a);
        startEngines();

        // Send the chain to node 2
        log.debug("****SEND CHAIN TO REMOTE NODE*********************************************************");
        try
        {
            SenderHelpers.sendApplication(a, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Launch chain
        log.debug("****START CHAIN***********************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Test finished nominally
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 32);
        Assert.assertEquals(32, res.size());
    }
}
