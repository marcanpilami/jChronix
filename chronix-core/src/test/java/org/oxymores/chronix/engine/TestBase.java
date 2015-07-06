package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestBase
{
    protected static Logger log = Logger.getLogger(TestBase.class);

    protected List<Application> applications = new ArrayList<>();
    protected Map<String, ChronixEngine> engines = new java.util.HashMap<String, ChronixEngine>();

    protected ChronixEngine addEngine(String database_path, Application a, String intname)
    {
        ChronixEngine e1 = new ChronixEngine(database_path, intname);
        e1.emptyDb();
        LogHelpers.clearAllTranscientElements(e1.ctx);

        // Save app in node 1
        try
        {
            e1.ctx.saveApplication(a);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try
        {
            e1.ctx.setWorkingAsCurrent(a);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        engines.put(intname, e1);
        return e1;
    }

    protected Application createTestApplication(String database_path, String name)
    {
        // Create a test application and save it inside context
        Application a1 = PlanBuilder.buildApplication(name, "test");

        // Physical network
        ExecutionNode en1 = PlanBuilder.buildExecutionNode(a1, "localhost", 1789);
        en1.setConsole(true);

        // Logical network
        Place p1 = PlanBuilder.buildPlace(a1, "master node", "master node", en1);

        PlanBuilder.buildPlaceGroup(a1, "master node", "master node", p1);
        PlanBuilder.buildPlaceGroup(a1, "all nodes", "all nodes", p1);

        // Chains and other stuff depends on the test

        // Done.
        this.applications.add(a1);
        return a1;
    }

    @Before
    public void prepare() throws Exception
    {
        // Always start a first engine
        String db1 = "C:\\TEMP\\db1";
        Application a = createTestApplication(db1, "test application");
        ChronixEngine e1 = addEngine(db1, a, "localhost:1789");

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
        for (ChronixEngine e : engines.values())
        {
            if (e.shouldRun())
            {
                e.stopEngine();
                e.waitForStopEnd();
            }
        }
    }

    public ChronixEngine firstEngine()
    {
        if (this.engines.size() != 1)
        {
            throw new RuntimeException("there are multiple engines running - there is not order between them");
        }
        return this.engines.values().iterator().next();
    }
}
