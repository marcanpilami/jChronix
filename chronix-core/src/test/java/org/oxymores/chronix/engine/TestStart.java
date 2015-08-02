package org.oxymores.chronix.engine;

import javax.jms.JMSException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestStart extends TestBase
{
    @Test
    public void testNoDb() throws Exception
    {
        log.info("***** Test: without a db, the scheduler fails with a adequate exception");
        ChronixEngine e = null;
        try
        {
            e = new ChronixEngine("C:\\WONTEXISTEVER", "local");
            e.start();
            e.waitForInitEnd();
        }
        catch (Exception ex)
        {
            return;
        }
        Assert.assertEquals(false, e.shouldRun()); // engine should not have been able to start
    }

    @Test
    public void testCreateAutoApplications() throws Exception
    {
        log.info("***** Test: with an empty db, the scheduler creates two auto applications");

        ChronixEngine e = addEngine(db1, "local");
        startEngines();

        Assert.assertEquals(2, e.ctx.getApplications().size());

        Application a1 = e.ctx.getApplicationByName("Operations");
        Application a2 = e.ctx.getApplicationByName("Chronix Maintenance");

        Assert.assertEquals(1, a1.getChains().size());
        Assert.assertEquals(1, a2.getChains().size());
        Assert.assertEquals(5, a2.getChains().get(0).getStates().size());
    }

    @Test
    public void testCalendarPointerCreationAtStartup() throws Exception
    {
        ChronixEngine e = addEngine(db1, "local");

        Application a = DemoApplication.getNewDemoApplication("localhost", 1789);
        addApplicationToDb(db1, a);
        startEngines();

        EntityManager em = e.ctx.getTransacEM();
        TypedQuery<CalendarPointer> q = em.createQuery("SELECT c from CalendarPointer c", CalendarPointer.class);
        Assert.assertEquals(1, q.getResultList().size());
    }

    @Test
    public void testRestart() throws JMSException
    {
        ChronixEngine e1 = addEngine(db1, "local");
        ChronixEngine e2 = addEngine(db2, "remote", "TransacUnit2", "HistoryUnit2");

        Application a = PlanBuilder.buildApplication("test", "description");
        n = new Network();
        ExecutionNode n1 = PlanBuilder.buildExecutionNode(n, "local", 1789);
        ExecutionNode n2 = PlanBuilder.buildExecutionNode(n, "remote", 1400);
        n1.connectTo(n2, NodeConnectionMethod.TCP);

        storeNetwork(db1, n);
        storeNetwork(db2, n);
        addApplicationToDb(db1, a);
        addApplicationToDb(db2, a);
        startEngines();

        SenderHelpers.sendApplication(a, n2, e1.ctx); // Send through 2 - it won't reboot
        e2.waitForRebootEnd();

        SenderHelpers.sendApplication(a, n1, e2.ctx);
        e1.waitForRebootEnd();

        SenderHelpers.sendApplication(a, n1, e2.ctx);
        e1.waitForRebootEnd();
    }
}
