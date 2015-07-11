package org.oxymores.chronix.engine;

import java.io.File;
import java.util.List;

import javax.jms.JMSException;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestHostedNode extends TestBase
{
    ChronixEngine e1;
    Application a;

    @Before
    public void before() throws Exception
    {
        a = PlanBuilder.buildApplication("test application", "");

        // Physical network
        n = new Network();
        ExecutionNode en1 = PlanBuilder.buildExecutionNode(n, "local", "localhost", 1789);
        ExecutionNode en2 = PlanBuilder.buildExecutionNode(n, "hosted", "localhost", 1804);
        en1.connectTo(en2, NodeConnectionMethod.RCTRL);

        // Logical network
        Place p1 = PlanBuilder.buildPlace(n, "master node", "master node", en1);
        Place p2 = PlanBuilder.buildPlace(n, "hosted node", "hosted node", en2);

        PlanBuilder.buildPlaceGroup(a, "master node", "master node", p1);
        PlanBuilder.buildPlaceGroup(a, "hosted node", "hosted node", p2);
        PlanBuilder.buildPlaceGroup(a, "all nodes", "all nodes", p1, p2);

        // Chains and other stuff are created by the tests themselves

        ChronixContext.saveNetwork(n, new File(db1));

        e1 = addEngine(db1, "local");
        addRunner(db2, "hosted", "localhost", 1804);
    }

    @Test
    public void testSimpleChain()
    {
        // Build a very simple chain
        Chain c1 = PlanBuilder.buildChain(a, "chain fully on hosted node", "simple chain", a.getGroup("hosted node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo oo", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("hosted node"), sc1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

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

        addApplicationToDb(db1, a);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

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

        addApplicationToDb(db1, a);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

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
