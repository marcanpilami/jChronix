package org.chronix.integration.tests;

import org.chronix.integration.helpers.BaseIT;
import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class TestGatesSingleNode extends BaseIT
{
    @Test
    public void testAndPassing()
    {
        // START -> NOOP1 |-> AND -> END
        // ......-> NOOP2 |
        DTOPlaceGroup pgLocal = app.getGroup("local");
        DTOEventSourceContainer c1 = new DTOEventSourceContainer(chainPrv, app, "c1", "integration test chain", null).setAllStates(pgLocal);

        DTOState s = plan1.addState(c1, pgLocal);
        DTOState s1 = c1.addState(noop, pgLocal);
        DTOState s2 = c1.addState(noop, pgLocal);
        DTOState s3 = c1.addState(and, pgLocal);

        c1.connect(getChainStart(c1), s1);
        c1.connect(getChainStart(c1), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, getChainEnd(c1));

        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);

        // Analysis
        waitForOk(6, 10, 200);
        checkHistory(6, 0);
    }

    @Test
    public void testAndBlocking()
    {
        // Should block at AND gate as FAIL1 always fails.
        // START -> NOOP1 |-> AND -> END
        // ......-> FAIL1 |
        DTOPlaceGroup pgLocal = app.getGroup("local");
        DTOEventSourceContainer c1 = new DTOEventSourceContainer(chainPrv, app, "c1", "integration test chain", null).setAllStates(pgLocal);

        DTOState s = plan1.addState(c1, app.getGroup("local"));
        DTOState s1 = c1.addState(noop, pgLocal);
        DTOState s2 = c1.addState(failure, pgLocal);
        DTOState s3 = c1.addState(and, pgLocal);

        c1.connect(getChainStart(c1), s1);
        c1.connect(getChainStart(c1), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, getChainEnd(c1));

        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);

        // Analysis
        waitForOk(6, 2); // should timeout
        checkHistory(2, 1);
    }

    @Test
    public void testOrPassing()
    {
        // START -> NOOP1 |-> OR -> END
        // ......-> FAIL1 |
        DTOPlaceGroup pgLocal = app.getGroup("local");
        DTOEventSourceContainer c1 = new DTOEventSourceContainer(chainPrv, app, "c1", "integration test chain", null).setAllStates(pgLocal);

        DTOState s = plan1.addState(c1, app.getGroup("local"));
        DTOState s1 = c1.addState(noop, pgLocal);
        DTOState s2 = c1.addState(failure, pgLocal);
        DTOState s3 = c1.addState(or, pgLocal);

        c1.connect(getChainStart(c1), s1);
        c1.connect(getChainStart(c1), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, getChainEnd(c1));

        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);

        // Analysis
        waitForEnded(6, 10, 500);
        checkHistory(5, 1);
    }
}
