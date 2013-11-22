package org.oxymores.chronix.engine;

import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestSim
{
    private static Logger log = Logger.getLogger(TestSim.class);

    @Test
    public void testSimpleChainOnce() throws Exception
    {
        log.info("Starting test testSimpleChainOnce");

        String dbPath = "C:\\TEMP\\db1";
        ChronixEngine e = new ChronixEngine(dbPath, "localhost:1789");
        e.emptyDb();
        LogHelpers.clearAllTranscientElements(e.ctx);

        // Create test application
        Application a = PlanBuilder.buildApplication("testing clocks", "no description for tests");
        PlaceGroup pgLocal = PlanBuilder.buildDefaultLocalNetwork(a, 1789, "localhost");
        Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);

        ClockRRule rr1 = PlanBuilder.buildRRule10Seconds(a);
        Clock ck1 = PlanBuilder.buildClock(a, "every 10 second", "every 10 second", rr1);
        // ck1.setDURATION(1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo aa", "aa", "should display 'aa'");

        State s1 = PlanBuilder.buildState(c, pgLocal, ck1);
        State s2 = PlanBuilder.buildState(c, pgLocal, sc1);
        s1.connectTo(s2);

        e.ctx.saveApplication(a);
        e.ctx.setWorkingAsCurrent(a);

        // Start the engine.
        e.start();

        // Now start the simulation
        List<RunLog> res = ChronixEngineSim.simulate(dbPath, a.getId(), DateTime.now(), DateTime.now().plusSeconds(10));
        Assert.assertEquals(1, res.size());

        // Stop the true engine...
        e.stopEngine();
        e.waitForStopEnd();
    }

}
