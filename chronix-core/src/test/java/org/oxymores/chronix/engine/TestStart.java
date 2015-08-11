package org.oxymores.chronix.engine;

import javax.jms.JMSException;

import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.sql2o.Connection;

public class TestStart extends TestBase
{
    @Test
    public void testNoDb() throws Exception
    {
        ChronixEngine e;
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
        // With an empty db, the scheduler creates two auto applications
        cleanDirectory(db1);
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

        try (Connection conn = e.ctx.getTransacDataSource().open())
        {
            int nbCals = conn.createQuery("SELECT COUNT(1) from CalendarPointer c").executeScalar(Integer.class);
            Assert.assertEquals(1, nbCals);
        }
    }

    @Test
    public void testRestart() throws JMSException
    {
        ChronixEngine e1 = addEngine(db1, "local");
        ChronixEngine e2 = addEngine(db2, "remote");

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
        e2.waitForInitEnd();

        SenderHelpers.sendApplication(a, n1, e2.ctx);
        e1.waitForInitEnd();

        SenderHelpers.sendApplication(a, n1, e2.ctx);
        e1.waitForInitEnd();
    }
}
