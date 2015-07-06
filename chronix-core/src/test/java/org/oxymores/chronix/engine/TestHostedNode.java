package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;
import javax.persistence.EntityManager;

import org.junit.Assert;
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
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestHostedNode extends TestBase
{
    ChronixEngine e1;

    @Before
    public void before() throws Exception
    {
        String db1 = "C:\\TEMP\\db1";
        String db2 = "C:\\TEMP\\db2";

        Application a = createTestApplication(db1, "test application");
        e1 = addEngine(db1, a, "localhost:1789");

        // Physical network
        ExecutionNode en1 = a.getNodesList().get(0);
        ExecutionNode en2 = PlanBuilder.buildExecutionNode(a, "localhost", 1804);
        en1.connectTo(en2, NodeConnectionMethod.RCTRL);

        // Logical network
        Place p1 = PlanBuilder.buildPlace(a, "master node", "master node", en1);
        Place p2 = PlanBuilder.buildPlace(a, "hosted node", "hosted node", en2);

        PlaceGroup pg1 = PlanBuilder.buildPlaceGroup(a, "master node", "master node", p1);
        PlaceGroup pg2 = PlanBuilder.buildPlaceGroup(a, "hosted node", "hosted node", p2);
        PlaceGroup pg3 = PlanBuilder.buildPlaceGroup(a, "all nodes", "all nodes", p1, p2);

        // Chains and other stuff are created by the tests themselves
        // Now save app in node 1
        try
        {
            e1.ctx.saveApplication(a);
            e1.ctx.setWorkingAsCurrent(a);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        //Create an empty test configuration db for second node
        ChronixEngine e2 = addRunner(db2, "localhost:1804");
        e2.emptyDb();

        startEngines();
    }

    @Test
    public void testSimpleChain()
    {
        Application a = this.applications.get(0);
        EntityManager em = e1.ctx.getTransacEM();

        // Build a very simple chain
        Chain c1 = PlanBuilder.buildChain(a, "chain fully on hosted node", "simple chain", a.getGroup("hosted node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo oo", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("hosted node"), sc1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());
        saveAndReloadApp(a, e1);

        // Run the chain
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 3);
        Assert.assertEquals(3, res.size());
    }

    @Test
    public void testMasterAndSlaveChain()
    {
        Application a = this.applications.get(0);
        EntityManager em = e1.ctx.getTransacEM();

        // Build the test chain
        Chain c1 = PlanBuilder.buildChain(a, "chain on both nodes", "simple chain", a.getGroup("hosted node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo a", "echoa", "a");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc1);

        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo b", "echob", "b");
        State s2 = PlanBuilder.buildState(c1, a.getGroup("hosted node"), sc2);

        ShellCommand sc3 = PlanBuilder.buildShellCommand(a, "echo c", "echoc", "c");
        State s3 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc3);

        c1.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        saveAndReloadApp(a, e1);

        // Run the chain
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 5);
        Assert.assertEquals(5, res.size());
    }

    @Test
    public void testMixedConditionChain()
    {
        Application a = this.applications.get(0);
        EntityManager em = e1.ctx.getTransacEM();

        // Build the test chain
        Chain c1 = PlanBuilder.buildChain(a, "chain on both nodes", "simple chain", a.getGroup("hosted node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo a", "echoa", "a");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc1);

        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo b", "echob", "b");
        State s2 = PlanBuilder.buildState(c1, a.getGroup("hosted node"), sc2);
        State s3 = PlanBuilder.buildStateAND(c1, a.getGroup("master node"));

        c1.getStartState().connectTo(s1);
        c1.getStartState().connectTo(s2);
        s1.connectTo(s3);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        saveAndReloadApp(a, e1);

        // Run the chain
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 5);
        Assert.assertEquals(5, res.size());
    }

}
