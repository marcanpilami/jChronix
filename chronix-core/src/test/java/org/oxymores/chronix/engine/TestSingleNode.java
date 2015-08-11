package org.oxymores.chronix.engine;

import java.util.List;

import javax.jms.JMSException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.CalendarBuilder;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.sql2o.Connection;

public class TestSingleNode extends TestBase
{
    Application a;
    ChronixEngine e1;

    @Before
    public void before() throws Exception
    {
        e1 = addEngine(db1, "local");

        a = PlanBuilder.buildApplication("test", "test");
        PlanBuilder.buildPlaceGroup(a, "master node", "master node", n.getPlace("local"));
    }

    @Test
    public void mainScenario() throws Exception
    {
        Calendar ca1 = CalendarBuilder.buildWeekDayCalendar(a, 2500);

        Chain c1 = PlanBuilder.buildChain(a, "simple chain using calendar", "chain2", a.getGroup("master node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo oo", "echo oo", "oo");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc1);
        s1.setCalendar(ca1);
        s1.setCalendarShift(-1);
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        Chain c2 = PlanBuilder.buildChain(a, "advance calendar chain", "chain3", a.getGroup("master node"));
        NextOccurrence no = PlanBuilder.buildNextOccurrence(a, ca1);
        State s2 = PlanBuilder.buildState(c2, a.getGroup("master node"), no);
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo ahahaha", "echo hahaha", "oo");
        State s3 = PlanBuilder.buildState(c2, a.getGroup("master node"), sc2);
        s3.setCalendar(ca1);

        addApplicationToDb(db1, a);
        startEngines();

        // Start chain
        log.debug("****FIRST (PASSING) RUN***************************************************************");
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 3);
        Assert.assertEquals(3, res.size());

        // Now, launch again. Should block after echo, for the calendar has not progressed.
        log.debug("****SECOND (BLOCKING - CALENDAR) RUN**************************************************");
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        Thread.sleep(2000); // Time to consume message

        res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(4, res.size());

        try (Connection conn = e1.ctx.getTransacDataSource().open())
        {
            List<CalendarPointer> q2 = conn.createQuery("SELECT * FROM CalendarPointer r").executeAndFetch(CalendarPointer.class);
            for (CalendarPointer c : q2)
            {
                log.debug(c.getRunning());
            }

            int nbEvents = conn.createQuery("SELECT COUNT(1) FROM Event e").executeScalar(Integer.class);
            Assert.assertEquals(1, nbEvents); // purge - only pending remain
        }

        // Now, advance calendar...
        log.debug("****CALENDAR UPDATE*******************************************************************");
        SenderHelpers.runStateInsidePlan(s2, e1.ctx);
        Thread.sleep(1000);

        // Test the event has been reanalyzed
        res = LogHelpers.waitForHistoryCount(e1.ctx, 7);
        Assert.assertEquals(7, res.size());

        // and do it again: the end of chain1 should not run.
        log.debug("****THIRD (BLOCKING - CALENDAR) RUN***************************************************");
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        Thread.sleep(2000); // Time to consume message

        res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(8, res.size());

        // and finally free the calendar, and test that state s3 is considered as straggling
        log.debug("****CALENDAR UPDATE*******************************************************************");
        SenderHelpers.runStateInsidePlan(s2, e1.ctx);
        Thread.sleep(2000);

        // Test stragglers
        try (Connection conn = e1.ctx.getTransacDataSource().open())
        {
            ca1.processStragglers(conn); // Display to ease debug
            Assert.assertEquals(1, ca1.getStragglers(conn).size());
        }

        // and test scheduling...
        res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(11, res.size());
    }

    @Test
    public void testAND()
    {
        // Build the test chain
        Chain c1 = PlanBuilder.buildChain(a, "chain on both nodes", "simple chain", a.getGroup("master node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo a", "echoa", "a");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc1);
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo b", "echob", "b");
        State s2 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc2);
        State s3 = PlanBuilder.buildStateAND(c1, a.getGroup("master node"));

        c1.getStartState().connectTo(s1);
        c1.getStartState().connectTo(s2);
        s1.connectTo(s3);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Run the chain
        log.debug("****START OF CHAIN1*******************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (JMSException e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 5);
        Assert.assertEquals(5, res.size());
    }

    @Test
    public void testANDWithBarrier()
    {
        // Build the test chains
        Calendar ca = CalendarBuilder.buildWeekDayCalendar(a, 2500);

        // First chain with the AND, with a State blocked by calendar
        Chain c1 = PlanBuilder.buildChain(a, "chain on both nodes", "simple chain", a.getGroup("master node"));
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo a", "echoa", "a");
        State s1 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc1);
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo b", "echob", "b");
        State s2 = PlanBuilder.buildState(c1, a.getGroup("master node"), sc2);
        State s3 = PlanBuilder.buildStateAND(c1, a.getGroup("master node"));

        s2.setCalendar(ca);
        s2.setCalendarShift(-1);

        c1.getStartState().connectTo(s1);
        c1.getStartState().connectTo(s2);
        s1.connectTo(s3);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        // Second chain to advance calendar
        Chain c2 = PlanBuilder.buildChain(a, "advance calendar chain", "chain3", a.getGroup("master node"));
        NextOccurrence no = PlanBuilder.buildNextOccurrence(a, ca);
        State s9 = PlanBuilder.buildState(c2, a.getGroup("master node"), no);
        c2.getStartState().connectTo(s9);
        s9.connectTo(c2.getEndState());

        addApplicationToDb(db1, a);
        startEngines();

        // Shift the state by 1 so that it cannot start (well, shouldn't)
        log.debug("****SHIFT CALENDAR********************************************************************");
        try
        {
            SenderHelpers.sendCalendarPointerShift(1, s2, e1.ctx);
            Thread.sleep(500);
        }
        catch (Exception e4)
        {
            e4.printStackTrace();
            Assert.fail(e4.getMessage());
        }

        // Run the chain
        log.debug("****ORDER START OF CHAIN1*************************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        List<RunLog> res = LogHelpers.waitForHistoryCount(e1.ctx, 2);
        Assert.assertEquals(2, res.size());

        try (Connection conn = e1.ctx.getTransacDataSource().open())
        {
            int nbEvents = conn.createQuery("SELECT COUNT(1) FROM Event e").executeScalar(Integer.class);
            Assert.assertEquals(2, nbEvents);
        }

        // Run second chain - should unlock the first chain
        log.debug("****ORDER START SECOND CHAIN**********************************************************");
        try
        {
            SenderHelpers.runStateInsidePlan(c2.getStartState(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        res = LogHelpers.waitForHistoryCount(e1.ctx, 8);
        Assert.assertEquals(8, res.size());
        try (Connection conn = e1.ctx.getTransacDataSource().open())
        {
            int nbEvents = conn.createQuery("SELECT COUNT(1) FROM Event e").executeScalar(Integer.class);
            Assert.assertEquals(0, nbEvents);
        }
    }

    @Test
    public void testExternal()
    {
        // The calendar
        Calendar ca1 = CalendarBuilder.buildWeekDayCalendar(a, 2500);
        NextOccurrence no = PlanBuilder.buildNextOccurrence(a, ca1);

        // The empty test chain that will be triggered by the external event
        Chain c1 = PlanBuilder.buildChain(a, "chain1", "simple chain 1", a.getGroup("master node"));
        c1.getStartState().connectTo(c1.getEndState()); // REALLY simple

        // The plan containing everything
        Chain p1 = PlanBuilder.buildPlan(a, "plan 1", "description");

        // Our file objects
        External pe1 = PlanBuilder.buildExternal(a, "file1", "^[a-zA-Z_/]*([0-9/]+).*");
        External pe2 = PlanBuilder.buildExternal(a, "file2", "^[a-zA-Z_/]*([0-9/]+).*");

        // First test case: calendar -> non calendar
        State sp1 = PlanBuilder.buildState(p1, a.getGroup("master node"), pe1);
        State sp2 = PlanBuilder.buildState(p1, a.getGroup("master node"), c1);
        sp1.connectTo(sp2, true);
        sp1.setCalendar(ca1);

        // Second test case: calendar -> same calendar
        State sp3 = PlanBuilder.buildState(p1, a.getGroup("master node"), pe2);
        State sp4 = PlanBuilder.buildState(p1, a.getGroup("master node"), c1);
        sp3.connectTo(sp4, true);
        sp3.setCalendar(ca1);
        sp4.setCalendar(ca1);

        // Calendar end of run state
        State sno = PlanBuilder.buildState(p1, a.getGroup("master node"), no);
        sno.setCalendar(ca1);
        sno.setEndOfOccurrence(true);

        String filepath = "/meuh/pouet/aaaa_12/06/2500";

        addApplicationToDb(db1, a);
        startEngines();

        // TEST 1: should block (no calendar on target state)
        log.debug("****START OF TEST1********************************************************************");
        try
        {
            SenderHelpers.sendOrderExternalEvent("file1", filepath, a.getGroup("master node").getPlaces().get(0).getNode(), e1.ctx);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e3)
        {
        }
        List<RunLog> res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(0, res.size());

        // TEST 2: should also block (good calendar, but wrong day)
        log.debug("****START OF TEST2********************************************************************");
        try
        {
            SenderHelpers.sendOrderExternalEvent("file2", filepath, a.getGroup("master node").getPlaces().get(0).getNode(), e1.ctx);
            Thread.sleep(2000);
        }
        catch (Exception e3)
        {
            Assert.fail(e3.getMessage());
        }

        res = LogHelpers.displayAllHistory(e1.ctx);
        Assert.assertEquals(0, res.size());

        log.debug("****START OF TEST3********************************************************************");
        try
        {
            SenderHelpers.sendCalendarPointerShift(200, ca1, e1.ctx);
            SenderHelpers.sendCalendarPointerShift(161, sp4, e1.ctx); // Chain1 is now at the right occurrence
        }
        catch (Exception e4)
        {
            e4.printStackTrace();
            Assert.fail(e4.getMessage());
        }

        res = LogHelpers.waitForHistoryCount(e1.ctx, 3);
        Assert.assertEquals(3, res.size());
    }
}
