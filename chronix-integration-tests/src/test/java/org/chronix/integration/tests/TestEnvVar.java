package org.chronix.integration.tests;

import org.chronix.integration.helpers.BaseIT;
import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOPlaceGroup;

/**
 * Single node, multiple places per group.
 */
public class TestEnvVar extends BaseIT
{
    /**
     * Tests env var transmission between states as well as auto env vars.
     */
    @Test
    public void testEnvVars()
    {
        // START -> GET TEST (fails) -> SET TEST -> GET TEST -> GET TEST -> GET CHR_JOBNAME -> END
        DTOPlaceGroup pgLocal = app.getGroup("local");
        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, app, "chain1", "integration test chain", null)
                .setAllStates(pgLocal);
        DTOState s = plan1.addState(chain1, pgLocal);

        DTOEventSource set = new DTOEventSource(setPrv, app, "set TEST variable", "test").setField("VARNAME", "TEST").setField("VALUE",
                "houba");
        DTOEventSource get = new DTOEventSource(getPrv, app, "get TEST variable", "test").setField("VARNAME", "TEST");
        DTOEventSource get2 = new DTOEventSource(getPrv, app, "get CHR_JOBNAME variable", "test").setField("VARNAME", "CHR_JOBNAME");

        DTOState s1 = chain1.addState(get, pgLocal);
        DTOState s2 = chain1.addState(set, pgLocal);
        DTOState s3 = chain1.addState(get, pgLocal);
        DTOState s4 = chain1.addState(get, pgLocal);
        DTOState s5 = chain1.addState(get2, pgLocal);

        chain1.connect(getChainStart(chain1), s1);
        chain1.connect(s1, s2).setGuard1(1);
        chain1.connect(s2, s3);
        chain1.connect(s3, s4);
        chain1.connect(s4, s5);
        chain1.connect(s5, getChainEnd(chain1));

        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);

        // Analysis
        waitForEnded(8, 10, 200);
        checkHistory(7, 1);
    }
}
