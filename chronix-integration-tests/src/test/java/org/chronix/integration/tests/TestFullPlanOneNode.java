package org.chronix.integration.tests;

import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.agent.command.api.RunnerConstants;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class TestFullPlanOneNode extends BaseIT
{
    @Test
    public void testPlan() throws InterruptedException
    {
        DTOPlaceGroup pgLocal = app.getGroup("local");

        // Application content
        // ShellCommand sc = new ShellCommand("c1", "c1", "echo", RunnerConstants.SHELL_WINCMD);
        DTOEventSource sc = new DTOEventSource(shellPrv, "c1", "c1", null);
        sc.setField(new DTOParameter("runnerCapability", RunnerConstants.SHELL_WINCMD));
        sc.setField(new DTOParameter("COMMAND", "echo"));
        sc.addParameter(new DTOParameter(null, "aa"));
        sc.addParameter(new DTOParameter(null, "bb"));
        app.addEventSource(sc);

        DTOEventSourceContainer c = (DTOEventSourceContainer) chainPrv.newInstance("first chain", "integration test chain", app, pgLocal);
        DTOState n1 = c.addState(sc, pgLocal);
        c.connect(getChainStart(c), n1);
        c.connect(n1, getChainEnd(c));

        // DTOState s = plan1.addState(c, app.getGroup("local"));

        save();

        // Tests
        meta.resetCache();
        DTOApplication a2 = meta.getApplication(app.getId());
        Assert.assertEquals("test app", a2.getName());

        // Assert.assertEquals(9, a2.getEventSources().size());
        /*
         * boolean found = false; for (DTOEventSource d : a2.getEventSources()) { if (d.getSource() instanceof Chain && "first chain"
         * .equals(((Chain) d.getSource()).getName())) { found = true; break; } } Assert.assertTrue(found);
         */

        addAndStartEngine("local");
        order.orderLaunch(a2.getId(), getChainStart(c).getId(), envt.getPlace("local").getId(), true);

        waitForOk(3, 10);
        checkHistory(3, 0);
    }
}
