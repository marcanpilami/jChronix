package org.oxymores.chronix.engine;

import javax.jms.JMSException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestStart
{
    private static Logger log = Logger.getLogger(TestStart.class);

    public static String db1;

    @Before
    public void init() throws Exception
    {
        db1 = "C:\\TEMP\\db1";
    }

    @Test
    public void testNoDb() throws Exception
    {
        log.info("***** Test: without a db, the scheduler fails with a adequate exception");
        ChronixEngine e = null;
        try
        {
            e = new ChronixEngine("C:\\WONTEXISTEVER", "localhost:1789");
            e.start();
            e.waitForInitEnd();
        }
        catch (Exception ex)
        {
            return;
        }
        Assert.assertEquals(false, e.shouldRun()); // engine should not have been able
        // to start
    }

    @Test
    public void testCreateAutoApplications() throws Exception
    {
        log.info("***** Test: with an empty db, the scheduler creates two auto applications");

        ChronixEngine e = new ChronixEngine("C:\\TEMP\\db1", "localhost:1789");
        e.emptyDb();
        LogHelpers.clearAllTranscientElements(e.ctx);

        e.start();
        e.waitForInitEnd();
        e.stopEngine();
        e.waitForStopEnd();

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
        ChronixEngine e = new ChronixEngine("C:\\TEMP\\db1", "localhost:1789");
        e.emptyDb();
        LogHelpers.clearAllTranscientElements(e.ctx);

        Application a = DemoApplication.getNewDemoApplication("localhost", 1789);
        ChronixContext ctx = ChronixContext.initContext("C:\\TEMP\\db1", "", "", "localhost:1789", false);
        ctx.saveApplication(a);
        ctx.setWorkingAsCurrent(a);
        e.start();
        e.waitForInitEnd();
        e.stopEngine();
        e.waitForStopEnd();

        EntityManager em = e.ctx.getTransacEM();
        TypedQuery<CalendarPointer> q = em.createQuery("SELECT c from CalendarPointer c", CalendarPointer.class);

        Assert.assertEquals(2, q.getResultList().size());
    }

    @Test
    public void testRestart() throws JMSException
    {
        ChronixEngine e1 = new ChronixEngine("C:\\TEMP\\db1", "localhost:1789");
        e1.emptyDb();
        LogHelpers.clearAllTranscientElements(e1.ctx);
        e1.start();
        e1.waitForInitEnd();

        ChronixEngine e2 = new ChronixEngine("C:\\TEMP\\db2", "localhost:1400");
        e2.emptyDb();
        LogHelpers.clearAllTranscientElements(e2.ctx);
        e2.start();
        e2.waitForInitEnd();

        Application a = PlanBuilder.buildApplication("test", "description");
        PlanBuilder.buildDefaultLocalNetwork(a, 1789, "localhost");
        ExecutionNode n1 = a.getNodesList().get(0);
        ExecutionNode n2 = PlanBuilder.buildExecutionNode(a, "localhost", 1400);
        n1.connectTo(n2, NodeConnectionMethod.TCP);

        SenderHelpers.sendApplication(a, n1, e1.ctx); // Send through 2 - it won't reboot
        e1.waitForRebootEnd();

        SenderHelpers.sendApplication(a, n1, e1.ctx);
        e1.waitForRebootEnd();

        SenderHelpers.sendApplication(a, n1, e1.ctx);
        e1.waitForRebootEnd();

        e1.stopEngine();
        e1.waitForStopEnd();
        e2.stopEngine();
        e2.waitForStopEnd();
    }
}
