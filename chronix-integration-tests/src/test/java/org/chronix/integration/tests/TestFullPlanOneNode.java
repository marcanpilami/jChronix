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
        DTOEventSource sc = new DTOEventSource(shellPrv, app, "c1", "c1").setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                .setField("COMMAND", "echo").addParameter("aa").addParameter("bb")
                .addParameter(new DTOParameter(null, strPrv).setField("value", "cc"));
        app.addEventSource(sc);

        DTOEventSourceContainer c = new DTOEventSourceContainer(chainPrv, app, "first chain", "integration test chain", null)
                .setAllStates(pgLocal);
        DTOState n1 = c.addState(sc, pgLocal);
        c.connect(getChainStart(c), n1);
        c.connect(n1, getChainEnd(c));

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
