package org.oxymores.chronix.engine;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
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

public class TestSim extends TestBase
{
    @Test
    public void testSimpleChainOnce() throws Exception
    {
        // Create test application
        Application a = PlanBuilder.buildApplication("testing clocks", "no description for tests");
        PlaceGroup pgLocal = PlanBuilder.buildPlaceGroup(a, "local", "local", n.getPlace("local"));
        Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);

        ClockRRule rr1 = PlanBuilder.buildRRule10Seconds(a);
        Clock ck1 = PlanBuilder.buildClock(a, "every 10 second", "every 10 second", rr1);
        // ck1.setDURATION(1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo aa", "aa", "should display 'aa'");

        State s1 = PlanBuilder.buildState(c, pgLocal, ck1);
        State s2 = PlanBuilder.buildState(c, pgLocal, sc1);
        s1.connectTo(s2);

        addApplicationToDb(db1, a);

        // Now start the simulation
        List<RunLog> res = ChronixEngineSim.simulate(db1, a.getId(), DateTime.now(), DateTime.now().plusSeconds(10));
        for (RunLog l : res)
        {
            log.info(l.getLine());
        }
        Assert.assertEquals(1, res.size());
    }
}
