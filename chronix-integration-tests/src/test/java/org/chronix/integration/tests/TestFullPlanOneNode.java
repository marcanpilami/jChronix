package org.chronix.integration.tests;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.agent.command.api.RunnerConstants;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.HistoryQuery;

public class TestFullPlanOneNode extends BaseIT
{
    @Test
    public void testPlan() throws InterruptedException
    {
        DTOPlaceGroup pgLocal = app.getGroup("local");

        // This tests dynamic parameter resolution with shell commands.
        // * aa: simple static parameter
        // * bb: same
        // * cc: a dynamic string parameter (its own parameter is static)
        // * dd: a dynamic shell parameter (its own parameters are static)
        // * ee: a dynamic shell parameter with a dynamic shell parameter defining its command (itself with static parameters)!
        DTOEventSource sc = new DTOEventSource(shellPrv, app, "c1", "c1").setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                .setField("COMMAND", "echo").addParameter("aa").addParameter("bb")
                .addParameter(new DTOParameter(null, strPrmPrv).setField("value", "cc"))
                .addParameter(new DTOParameter(null, shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                        .setField("COMMAND", "echo").addAdditionalParameter("dd"))
                .addParameter(new DTOParameter(null, shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                        .setField(new DTOParameter("COMMAND", shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                                .setField("COMMAND", "echo").addAdditionalParameter("echo"))
                        .addAdditionalParameter("ee"));
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

        HistoryQuery q = new HistoryQuery();
        q.setResultCode(0);
        List<DTORunLog> res = history.query(q).getRes();
        boolean found = false;
        for (DTORunLog l : res)
        {
            if (l.getActiveNodeName().equals("c1"))
            {
                found = true;
                Assert.assertEquals("aa bb cc dd ee", l.getShortLog());
                break;
            }
        }
        Assert.assertTrue(found);
    }
}
