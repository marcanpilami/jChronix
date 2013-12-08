package org.oxymores.chronix.engine;

import javax.persistence.EntityManager;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestPurge
{
    private static Logger log = Logger.getLogger(TestPurge.class);

    private String db1;
    Application a1;
    ChronixEngine e1;
    ExecutionNode en1;
    Place p1;
    PlaceGroup pg1, pg2;

    @Before
    public void prepare() throws Exception
    {
        db1 = "C:\\TEMP\\db1";

        /************************************************
         * Create a test configuration db
         ***********************************************/

        e1 = new ChronixEngine(db1, "localhost:1789");
        e1.emptyDb();
        LogHelpers.clearAllTranscientElements(e1.ctx);

        // Create a test application and save it inside context
        a1 = PlanBuilder.buildApplication("Purge test, single node", "test");

        // Physical network
        en1 = PlanBuilder.buildExecutionNode(a1, "localhost", 1789);
        en1.setConsole(true);

        // Logical network
        p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);

        pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
        pg2 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1);

        // Chains and other stuff depends on the test

        // Save app in node 1
        try
        {
            e1.ctx.saveApplication(a1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try
        {
            e1.ctx.setWorkingAsCurrent(a1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        /************************************************
         * Start the engine
         ***********************************************/

        e1.start();
        log.debug("Started - begin waiting");
        e1.waitForInitEnd();
        log.debug("Engines inits done");
    }

    @After
    public void cleanup()
    {
        log.debug("**************************************************************************************");
        log.debug("****END OF TEST***********************************************************************");
        if (e1 != null && e1.shouldRun())
        {
            e1.stopEngine();
            e1.waitForStopEnd();
        }
    }

    @Test
    public void testPurgeAfterDeleteApp() throws Exception
    {
        log.debug("**************************************************************************************");
        log.debug("****CREATE PLAN***********************************************************************");

        EntityManager em = e1.ctx.getTransacEM();

        // A chain that will never be able to finish
        Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");

        State s1 = PlanBuilder.buildState(c1, pg1, sc1);
        State s2 = PlanBuilder.buildState(c1, pg1, sc1);
        State s3 = PlanBuilder.buildStateAND(c1, pg1);

        c1.getStartState().connectTo(s1);
        s1.connectTo(s3);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        // Save plan
        try
        {
            SenderHelpers.sendApplicationToAllClients(a1, e1.getContext());
            e1.waitForRebootEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Start chain
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);

        // Wait for end of run
        Thread.sleep(5000);

        // Check some events exist inside the database
        Assert.assertEquals(1L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Create a new app, remove the old one.
        e1.ctx.deleteCurrentApplication(a1);
        Application a2 = PlanBuilder.buildApplication("test2", "description");
        PlanBuilder.buildExecutionNode(a2, "localhost", 1789);
        SenderHelpers.sendApplicationToAllClients(a2, e1.getContext());
        e1.waitForRebootEnd();

        Assert.assertEquals(0L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Done
        em.close();
    }

    @Test
    public void testPurgeAfterChangedApp() throws Exception
    {
        log.debug("**************************************************************************************");
        log.debug("****CREATE PLAN***********************************************************************");

        EntityManager em = e1.ctx.getTransacEM();

        // A chain that will never be able to finish
        Chain c1 = PlanBuilder.buildChain(a1, "simple chain", "chain1", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a1, "echo oo", "echo oo", "oo");

        State s1 = PlanBuilder.buildState(c1, pg1, sc1);
        State s2 = PlanBuilder.buildState(c1, pg1, sc1);
        State s3 = PlanBuilder.buildStateAND(c1, pg1);

        c1.getStartState().connectTo(s1);
        s1.connectTo(s3);
        s2.connectTo(s3);
        s3.connectTo(c1.getEndState());

        // Save plan
        try
        {
            SenderHelpers.sendApplicationToAllClients(a1, e1.getContext());
            e1.waitForRebootEnd();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        // Start chain
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);

        // Wait for end of run
        Thread.sleep(5000);

        // Check some events exist inside the database
        Assert.assertEquals(1L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Remove the chain from the application. Events from this chain should be purged.
        a1.removeActiveElement(c1);
        SenderHelpers.sendApplicationToAllClients(a1, e1.getContext());
        e1.waitForRebootEnd();

        Assert.assertEquals(0L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Done
        em.close();
    }

    // TODO: add tests for Token & clocktick purge.
}
