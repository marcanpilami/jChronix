package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.RunnerCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.CalendarBuilder;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestMultiNode extends TestBase
{
    ChronixEngine e1, e2, e3;
    Application a1;
    ExecutionNode en1, en2, en3;
    Place p1, p2, p3;
    PlaceGroup pg1, pg2, pg3, pg4;

    @Before
    public void before() throws Exception
    {
        e1 = addEngine(db1, "e1");
        e2 = addEngine(db2, "e2");
        e3 = addRunner(db3, "e3", "localhost", 1804);

        // Create a test application and save it inside context
        a1 = PlanBuilder.buildApplication("Multinode test", "test");

        // Physical network
        n = new Environment();
        en1 = PlanBuilder.buildExecutionNode(n, "e1", "localhost", 1789);
        en2 = PlanBuilder.buildExecutionNode(n, "e2", "localhost", 1400);
        en3 = PlanBuilder.buildExecutionNode(n, "e3", "localhost", 1804);
        n.setConsole(en1);
        en1.connectTo(en2, ExecutionNodeConnectionAmq.class);
        en2.connectTo(en3, ExecutionNodeConnectionAmq.class);
        en3.setComputingNode(en2);
        e2.setFeeder(en1);

        // Logical network
        p1 = PlanBuilder.buildPlace(n, "master node", en1);
        p2 = PlanBuilder.buildPlace(n, "second node", en2);
        p3 = PlanBuilder.buildPlace(n, "hosted node by second node", en3);

        pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
        pg2 = PlanBuilder.buildPlaceGroup(a1, "second node", "second node", p2);
        pg3 = PlanBuilder.buildPlaceGroup(a1, "hosted node by second node", "third node", p3);
        pg4 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1, p2, p3);

        storeEnvironment(db1, n);

        // Chains and other stuff depends on the test
    }

    @Test
    public void testSend()
    {
        storeEnvironment(db2, n); // Avoid some restarts
        addApplicationToDb(db1, a1);
        startEngines();
        try
        {
            SenderHelpers.sendApplication(e1.ctx.getApplicationByName("Multinode test"), en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        Application a2 = null;
        try
        {
            a2 = e2.ctx.getApplicationByName("Multinode test");
        }
        catch (Exception e)
        {
            Assert.fail();
        }

        Assert.assertEquals(3, e2.ctx.getEnvironment().getPlaces().values().size());
        Assert.assertEquals(0, a2.getChains().size());
    }

    @Test
    public void testSimpleChain()
    {
        // Build a very simple chain
        Chain c1 = PlanBuilder.buildChain(a1, "empty chain", "empty chain", pg1);
        RunnerCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, pg2, sc1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        addApplicationToDb(db1, a1);
        startEngines();

        // Node 2 will fetch applications from node 1 as it has no metadata and then reboot
        e2.waitForInitEnd();

        // Test reception is OK
        Application a2 = e2.ctx.getApplicationByName("Multinode test");
        if (a2 == null)
        {
            Assert.fail("No application in remote context after reception");
        }

        Assert.assertEquals(3, e2.ctx.getEnvironment().getPlaces().values().size());
        Assert.assertEquals(1, a2.getChains().size());

        // Run the chain
        try
        {
            SenderHelpers.runStateInsidePlanWithoutCalendarUpdating(c1.getStartState(), p1, e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 3);
        Assert.assertEquals(3, res.size());
    }

    @Test
    public void testCalendarTransmission()
    {
        storeEnvironment(db2, n); // Avoid some restarts

        Calendar ca = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

        Chain c1 = PlanBuilder.buildChain(a1, "simple chain using calendar", "chain2", pg2);
        RunnerCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, pg2, sc1);
        s1.setCalendar(ca);
        s1.setCalendarShift(-1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
        NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca);
        State s2 = PlanBuilder.buildState(c2, pg1, no);
        c2.getStartState().connectTo(s2);
        s2.connectTo(c2.getEndState());

        log.debug("****SAVE CHAIN************************************************************************");
        addApplicationToDb(db1, a1);
        startEngines();

        log.debug("****SEND CHAIN************************************************************************");
        try
        {
            SenderHelpers.sendApplication(a1, en2, e1.ctx);
            e2.waitForInitEnd();
            log.debug("Application integration should be over by now...");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Test reception is OK
        Application a2 = e2.ctx.getApplicationByName("Multinode test");
        if (a2 == null)
        {
            Assert.fail("No application in remote context after reception");
        }
        Assert.assertEquals(3, e2.ctx.getEnvironment().getPlaces().values().size());
        LogHelpers.testCalendarPointerCount(e1.ctx, 2);
        LogHelpers.testCalendarPointerCount(e2.ctx, 2);

        log.debug("****SHIFT CALENDAR********************************************************************");
        // Shift the state by 1 so that it cannot start (well, shouldn't)
        try
        {
            SenderHelpers.sendCalendarPointerShift(1, s1, e1.ctx);
            Thread.sleep(1000);
        }
        catch (Exception e4)
        {
            e4.printStackTrace();
            Assert.fail(e4.getMessage());
        }
        LogHelpers.testCalendarPointerCount(e1.ctx, 2);
        LogHelpers.testCalendarPointerCount(e2.ctx, 2);

        // Run the first chain - should be blocked after starting State
        log.debug("****ORDER START FIRST CHAIN***********************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        sleep(2);
        List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(1, res.size());

        // Run second chain - should unlock the first chain on the other node
        log.debug("****ORDER START SECOND CHAIN**********************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Have all jobs run?
        res = LogHelpers.waitForHistoryCount(e1.ctx, 6);
        Assert.assertEquals(6, res.size());

        // Has purge worked?
        LogHelpers.testEventCount(e2.ctx, 0);
        LogHelpers.testEventCount(e1.ctx, 0);
    }

    @Test
    public void testRemoteHostedAgent()
    {
        log.debug("****CREATE CHAIN**********************************************************************");
        Calendar ca = CalendarBuilder.buildWeekDayCalendar(a1, 2500);

        Chain c1 = PlanBuilder.buildChain(a1, "AND plus CALENDAR", "chain2", pg2);
        RunnerCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo aa", "echo aa", "aa");
        State s1 = PlanBuilder.buildState(c1, pg1, sc1);
        RunnerCommand sc2 = PlanBuilder.buildShellCommand(a1, "echo bb", "echo bb", "bb");
        State s2 = PlanBuilder.buildState(c1, pg2, sc2);
        RunnerCommand sc3 = PlanBuilder.buildShellCommand(a1, "echo cc", "echo cc", "cc");
        State s3 = PlanBuilder.buildState(c1, pg3, sc3);
        RunnerCommand sc4 = PlanBuilder.buildShellCommand(a1, "echo dd", "echo dd", "dd");
        State s4 = PlanBuilder.buildState(c1, pg1, sc4);
        State s5 = PlanBuilder.buildStateAND(c1, pg3);

        s4.setCalendar(ca);
        s4.setCalendarShift(-1);

        c1.getStartState().connectTo(s1);
        c1.getStartState().connectTo(s2);
        c1.getStartState().connectTo(s3);

        s1.connectTo(s5);
        s2.connectTo(s5);
        s3.connectTo(s5);

        s5.connectTo(s4);
        s4.connectTo(c1.getEndState());

        Chain c2 = PlanBuilder.buildChain(a1, "advance calendar chain", "chain3", pg1);
        NextOccurrence no = PlanBuilder.buildNextOccurrence(a1, ca);
        State s9 = PlanBuilder.buildState(c2, pg1, no);
        c2.getStartState().connectTo(s9);
        s9.connectTo(c2.getEndState());

        log.debug("****SAVE CHAIN************************************************************************");
        addApplicationToDb(db1, a1);
        storeEnvironment(db2, n); // Avoid some restarts
        startEngines();

        log.debug("****SEND CHAIN************************************************************************");
        try
        {
            SenderHelpers.sendApplication(a1, en2, e1.ctx);
            e2.waitForInitEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail();
        }

        // Test reception is OK
        Application a2 = e2.ctx.getApplicationByName("Multinode test");
        if (a2 == null)
        {
            Assert.fail("No application in remote context after reception");
        }
        Assert.assertEquals(3, e2.ctx.getEnvironment().getPlaces().values().size());

        LogHelpers.testCalendarPointerCount(e1.ctx, 2);

        log.debug("****SHIFT CALENDAR********************************************************************");
        // Shift the state by 1 so that it cannot start (well, shouldn't)
        try
        {
            SenderHelpers.sendCalendarPointerShift(1, s4, e1.ctx);
            Thread.sleep(500);
        }
        catch (Exception e4)
        {
            e4.printStackTrace();
            Assert.fail(e4.getMessage());
        }
        LogHelpers.testCalendarPointerCount(e1.ctx, 2);
        LogHelpers.testCalendarPointerCount(e2.ctx, 2);

        // Run the first chain - should be blocked after the AND
        log.debug("****ORDER START FIRST CHAIN***********************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 5);
        sleep(1);
        Assert.assertEquals(5, res.size());

        // Run second chain - should unlock the first chain on the other node
        log.debug("****ORDER START SECOND CHAIN**********************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        // Have all jobs run?
        res = LogHelpers.waitForHistoryCount(e1.ctx, 10);
        Assert.assertEquals(10, res.size());

        // Has purge worked?
        LogHelpers.testEventCount(e1.ctx, 0);
        LogHelpers.testEventCount(e2.ctx, 0);
    }
}
