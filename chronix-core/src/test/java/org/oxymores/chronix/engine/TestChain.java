package org.oxymores.chronix.engine;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestChain extends TestBase
{
    ChronixEngine e1;
    Application a1;

    @Before
    public void before() throws Exception
    {
        a1 = addTestApplicationToDb(db1, "test1");
        e1 = addEngine(db1, "local");
    }

    @Test
    public void testChainLaunch() throws Exception
    {
        log.debug("****CREATE PLAN***********************************************************************");
        PlaceGroup pg1 = a1.getGroup("master node");

        // First stupid chain
        Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "powershell.exe -Command \"Start-Sleep 1\"", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, pg1, sc1);

        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        // Plan that contains the other chains
        Chain p1 = PlanBuilder.buildPlan(a1, "main plan", "nothing important");
        State sp = PlanBuilder.buildState(p1, pg1, c1);

        // GO
        addApplicationToDb(db1, a1);
        startEngines();

        log.debug("****PASSING RUN***************************************************************");
        SenderHelpers.runStateInsidePlan(sp, firstEngine().ctx);

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.getContext(), 4);
        LogHelpers.displayAllHistory(e1.getContext());
        Assert.assertEquals(4, res.size());
        RunLog rl0 = res.get(0);
        RunLog rl3 = res.get(3);
        DateTime end0 = rl0.getStoppedRunningAt();
        DateTime end3 = rl3.getStoppedRunningAt();

        Assert.assertEquals("simple chain", rl0.getActiveNodeName());
        Assert.assertTrue(end0.isAfter(end3) || end0.isEqual(end3));
    }

    @Test
    public void testCompletePlan() throws Exception
    {
        log.debug("****CREATE PLAN***********************************************************************");
        PlaceGroup pg1 = a1.getGroup("master node");

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

        // GO
        addApplicationToDb(db1, a1);
        startEngines();

        // Start chain
        log.debug("****PASSING RUN***************************************************************");
        SenderHelpers.runStateInsidePlan(sp1, e1.ctx);

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 17);
        Assert.assertEquals(17, res.size());
        RunLog rl0 = res.get(0);

        Assert.assertEquals("simple chain 1", rl0.getActiveNodeName());
    }

    @Test
    public void testDisabled() throws Exception
    {
        log.debug("****CREATE PLAN***********************************************************************");
        PlaceGroup pg1 = a1.getGroup("master node");

        // First stupid chain
        Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo a", "echo a", "oo");
        State s1 = PlanBuilder.buildState(c1, pg1, sc1);
        s1.setEnabled(false);

        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        // GO
        addApplicationToDb(db1, a1);
        startEngines();

        log.debug("****PASSING RUN***************************************************************");
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.getContext());

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.getContext(), 3);
        LogHelpers.displayAllHistory(e1.getContext());
        Assert.assertEquals(3, res.size());

        Assert.assertEquals("was not run as it was marked as disabled", res.get(0).getShortLog() + res.get(1).getShortLog() + res.get(2).getShortLog());
    }
}
