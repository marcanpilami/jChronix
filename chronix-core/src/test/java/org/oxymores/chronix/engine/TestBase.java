package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestBase
{
    protected static Logger log = Logger.getLogger(TestBase.class);

    @Rule
    public TestName testName = new TestName();

    protected List<Application> applications = new ArrayList<>();
    protected Map<String, ChronixEngine> engines = new java.util.HashMap<>();

    @Before
    public void prepare() throws Exception
    {
        log.debug("****BEGINNING OF TEST " + testName.getMethodName() + "***********************************************************************");
    }

    @After
    public void cleanup()
    {
        log.debug("****END OF TEST " + testName.getMethodName() + "***********************************************************************");
        for (ChronixEngine e : engines.values())
        {
            if (e.shouldRun())
            {
                e.stopEngine();
            }
        }
        for (ChronixEngine e : engines.values())
        {
            if (e.shouldRun())
            {
                e.waitForStopEnd();
            }
        }
    }

    protected ChronixEngine addRunner(String database_path, String intname)
    {
        // Unit names don't really matter here - runners don't use them.
        ChronixEngine e = new ChronixEngine(database_path, intname, "TransacUnitXXX", "HistoryUnitXXX", true);
        e.emptyDb();

        engines.put(intname, e);
        return e;
    }

    protected ChronixEngine addEngine(String database_path, Application a, String intname)
    {
        return addEngine(database_path, a, intname, "TransacUnit", "HistoryUnit");
    }

    protected ChronixEngine addEngine(String database_path, Application a, String intname, String transacUnitName, String histUnitName)
    {
        ChronixEngine e = new ChronixEngine(database_path, intname, transacUnitName, histUnitName);
        e.emptyDb();
        LogHelpers.clearAllTranscientElements(e.ctx);

        // Save app in node 1
        try
        {
            e.ctx.saveApplication(a);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        try
        {
            e.ctx.setWorkingAsCurrent(a);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }

        engines.put(intname, e);
        return e;
    }

    protected void startEngines()
    {
        for (ChronixEngine e : this.engines.values())
        {
            e.start();
        }
        for (ChronixEngine e : this.engines.values())
        {
            e.waitForInitEnd();
        }
    }

    protected Application createTestApplication(String database_path, String name)
    {
        // Create a test application and save it inside context
        Application a = PlanBuilder.buildApplication(name, "test");

        // Physical network: one node
        ExecutionNode en1 = PlanBuilder.buildExecutionNode(a, "localhost", 1789);
        en1.setConsole(true);

        // Logical network
        Place p1 = PlanBuilder.buildPlace(a, "master node", "master node", en1);

        PlanBuilder.buildPlaceGroup(a, "master node", "master node", p1);
        PlanBuilder.buildPlaceGroup(a, "all nodes", "all nodes", p1);

        // Chains and other stuff depends on the test
        // Done.
        this.applications.add(a);
        return a;
    }

    public ChronixEngine firstEngine()
    {
        if (this.engines.size() != 1)
        {
            throw new RuntimeException("there are multiple engines running - there is not order between them");
        }
        return this.engines.values().iterator().next();
    }

    public void saveAndReloadApp(Application a, ChronixEngine e)
    {
        try
        {
            e.ctx.saveApplication(a);
            e.ctx.setWorkingAsCurrent(a);
            e.queueReloadConfiguration();
            e.waitForInitEnd();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            Assert.fail(ex.getMessage());
        }
    }
}
