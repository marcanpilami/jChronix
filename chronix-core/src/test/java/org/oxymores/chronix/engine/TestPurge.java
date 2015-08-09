package org.oxymores.chronix.engine;

import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestPurge extends TestBase
{
    Application a1;
    ChronixEngine e1;
    ExecutionNode en1;
    Place p1;
    PlaceGroup pg1, pg2;

    @Before
    public void prepare() throws Exception
    {
        e1 = addEngine(db1, "e1");

        // Physical network
        n = new Network();
        en1 = PlanBuilder.buildExecutionNode(n, "e1", "localhost", 1789);
        en1.setConsole(true);

        // Logical network
        p1 = PlanBuilder.buildPlace(n, "master node", en1);
        storeNetwork(db1, n);

        // App
        a1 = PlanBuilder.buildApplication("Purge test, single node", "test");
        pg1 = PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
        pg2 = PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1);
    }

    @Test
    public void testPurgeAfterDeleteApp() throws Exception
    {
        log.debug("****CREATE PLAN***********************************************************************");
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

        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);

        // Wait for end of run
        sleep(5);

        // Check some events exist inside the database
        Assert.assertEquals(1L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Create a new app, remove the old one.
        e1.ctx.deleteCurrentApplication(a1);
        Application a2 = PlanBuilder.buildApplication("test2", "description");
        SenderHelpers.sendApplicationToAllClients(a2, e1.getContext());
        e1.waitForInitEnd();
        Assert.assertEquals(0L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Done
        em.close();
    }

    @Test
    public void testPurgeAfterChangedApp() throws Exception
    {
        log.debug("****CREATE PLAN***********************************************************************");
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
        addApplicationToDb(db1, a1);
        startEngines();
        EntityManager em = e1.ctx.getTransacEM();

        // Start chain
        SenderHelpers.runStateInsidePlan(c1.getStartState(), e1.ctx, em);

        // Wait for end of run
        Thread.sleep(5000);

        // Check some events exist inside the database
        Assert.assertEquals(1L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Remove the chain from the application. Events from this chain should be purged.
        a1.removeActiveElement(c1);
        SenderHelpers.sendApplicationToAllClients(a1, e1.getContext());
        e1.waitForInitEnd();

        Assert.assertEquals(0L, em.createQuery("SELECT COUNT(tb) FROM Event tb", Long.class).getSingleResult().longValue());

        // Done
        em.close();
    }

    // TODO: add tests for Token & clocktick purge.
}
