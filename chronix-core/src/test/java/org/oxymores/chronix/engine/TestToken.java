package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestToken extends TestBase
{
    Application a1;
    PlaceGroup pg1, pg2;
    ChronixEngine e1;

    @Before
    public void before() throws Exception
    {
        e1 = addEngine(db1, "local");

        PlanBuilder.buildPlace(n, "second", n.getNode("local"));
        storeNetwork(db1, n);

        a1 = PlanBuilder.buildApplication("Single node test", "test");
        pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", n.getPlace("local"));
        pg2 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", n.getPlace("local"), n.getPlace("second"));
    }

    @Test
    public void testAlone() throws Exception
    {
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

        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        log.debug("**** RUN *****************************************************************************");
        SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);

        // Tests
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 4);
        sleep(1); // More?
        Assert.assertEquals(4, res.size());
        RunLog rl0 = res.get(0);
        RunLog rl3 = res.get(3);
        DateTime end0 = new DateTime(rl0.getStoppedRunningAt());
        DateTime end3 = new DateTime(rl3.getStoppedRunningAt());

        Assert.assertEquals(rl0.getActiveNodeName(), "simple chain");
        Assert.assertTrue(end0.isAfter(end3) || end0.isEqual(end3));
    }

    @Test
    public void testAloneBlocking() throws Exception
    {
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

        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        log.debug("**** RUN *****************************************************************************");
        SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
        Thread.sleep(5000); // Time to consume message - long so as to allow more than expected

        // Tests
        List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(1, res.size());
    }

    @Test
    public void testTwoWithOne() throws Exception
    {
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

        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        log.debug("*** RUN ******************************************************************************");
        SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
        LogHelpers.waitForHistoryCount(e1.ctx, 5);

        // Tests
        List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(5, res.size());
        RunLog rl2 = res.get(2);
        RunLog rl3 = res.get(3);
        DateTime end2 = new DateTime(rl2.getStoppedRunningAt());
        DateTime start2 = new DateTime(rl3.getBeganRunningAt());

        Assert.assertTrue(start2.isAfter(end2));
    }

    @Test
    public void testTwoWithOnePerPlace() throws Exception
    {
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

        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        log.debug("**** RUN *****************************************************************************");
        SenderHelpers.runStateInsidePlan(sp, e1.ctx, em);
        Thread.sleep(9000); // Time to consume message

        // Tests
        List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(5, res.size());
        RunLog rl2 = res.get(2);
        RunLog rl3 = res.get(3);
        DateTime end3 = new DateTime(rl3.getStoppedRunningAt());
        DateTime start3 = new DateTime(rl3.getBeganRunningAt());
        DateTime end2 = new DateTime(rl2.getStoppedRunningAt());
        DateTime start2 = new DateTime(rl2.getBeganRunningAt());

        Assert.assertTrue((end2.isBefore(start3) || end2.equals(start3)) || (end3.isBefore(start2) || end3.equals(start2)));
    }
}
