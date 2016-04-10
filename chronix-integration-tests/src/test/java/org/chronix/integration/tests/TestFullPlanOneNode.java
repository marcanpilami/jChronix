package org.chronix.integration.tests;

import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.api.source2.DTOEventSourceContainer;
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

        DTOEventSourceContainer c = (DTOEventSourceContainer) chainPrv.newInstance("first chain", "integration test chain", app, pgLocal);
        // DTOState n1 = c.addState(sc);
        c.connect(getChainStart(c), getChainEnd(c));
        // c.connect(n1, c.getEnd());

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

        waitForOk(2, 10);
        checkHistory(2, 0);
    }
}
