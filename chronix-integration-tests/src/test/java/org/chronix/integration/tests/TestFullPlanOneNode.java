package org.chronix.integration.tests;

import java.util.List;

import org.chronix.integration.helpers.BaseIT;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.agent.command.api.RunnerConstants;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.HistoryQuery;

public class TestFullPlanOneNode extends BaseIT
{
    @Test
    public void testPlan() throws InterruptedException
    {
        DTOPlaceGroup pgLocal = app.getGroup("local");

        // Full plan testing
        // Plan: External1 -> Chain1 -> Noop1
        // Chain1: Start -> shell1 -> End (this also tests dynamic parameters)

        ///////////
        // Chain1

        // Shell1: this tests dynamic parameter resolution with shell commands.
        // * aa: simple static parameter
        // * bb: same
        // * cc: a dynamic string parameter (its own parameter is static)
        // * dd: a dynamic shell parameter (its own parameters are static)
        // * ee: a dynamic shell parameter with a dynamic shell parameter defining its command (itself with static parameters)!
        DTOEventSource shell1 = new DTOEventSource(shellPrv, app, "shell1", "shell1")
                .setField("runnerCapability", RunnerConstants.SHELL_WINCMD).setField("COMMAND", "echo").addParameter("aa")
                .addParameter("bb").addParameter(new DTOParameter(null, strPrmPrv).setField("value", "cc"))
                .addParameter(new DTOParameter(null, shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                        .setField("COMMAND", "echo").addAdditionalParameter("dd"))
                .addParameter(new DTOParameter(null, shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                        .setField(new DTOParameter("COMMAND", shellPrmPrv).setField("runnerCapability", RunnerConstants.SHELL_WINCMD)
                                .setField("COMMAND", "echo").addAdditionalParameter("echo"))
                        .addAdditionalParameter("ee"));
        app.addEventSource(shell1);

        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, app, "first chain", "integration test chain", null)
                .setAllStates(pgLocal);
        app.addEventSource(chain1);

        DTOState n1 = chain1.addState(shell1, pgLocal);
        chain1.connect(getChainStart(chain1), n1);
        chain1.connect(n1, getChainEnd(chain1));

        ///////////
        // Plan main content

        DTOEventSource external1 = new DTOEventSource(extPrv, app, "external1", "external1");
        app.addEventSource(external1);

        DTOState external1_state = plan1.addState(external1, pgLocal);
        DTOState chain1_state = plan1.addState(chain1, pgLocal);
        DTOState noop1_state = plan1.addState(noop, pgLocal);

        plan1.connect(external1_state, chain1_state);
        plan1.connect(chain1_state, noop1_state);

        // Meta is done.
        save();

        // Launch (from external event)
        addAndStartEngine("local");
        order.orderExternal(external1.getName(), null);

        // Global test
        waitForOk(6, 10);
        checkHistory(6, 0);

        // Prm value tests
        HistoryQuery q = new HistoryQuery();
        q.setResultCode(0);
        List<DTORunLog> res = history.query(q).getRes();
        boolean found = false;
        for (DTORunLog l : res)
        {
            if (l.getActiveNodeName().equals("shell1"))
            {
                found = true;
                Assert.assertEquals("aa bb cc dd ee", l.getShortLog());
                break;
            }
        }
        Assert.assertTrue(found);
    }
}
