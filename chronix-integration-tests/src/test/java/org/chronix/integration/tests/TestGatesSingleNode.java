package org.chronix.integration.tests;

import org.junit.Test;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.source.chain.dto.Chain;

public class TestGatesSingleNode extends BaseIT
{
    @Test
    public void testAndPassing()
    {
        // START -> NOOP1 |-> AND -> END
        // ......-> NOOP2 |
        Chain c1 = new Chain("c1", "c1", app.getGroup("local"));
        app.addEventSource(c1);
        DTOState s = plan1.addState(c1, app.getGroup("local"));

        DTOState s1 = c1.addState(noop);
        DTOState s2 = c1.addState(noop);
        DTOState s3 = c1.addState(and);

        c1.connect(c1.getStart(), s1);
        c1.connect(c1.getStart(), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, c1.getEnd());

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
        Chain c1 = new Chain("c1", "c1", app.getGroup("local"));
        app.addEventSource(c1);
        DTOState s = plan1.addState(c1, app.getGroup("local"));

        DTOState s1 = c1.addState(noop);
        DTOState s2 = c1.addState(failure);
        DTOState s3 = c1.addState(and);

        c1.connect(c1.getStart(), s1);
        c1.connect(c1.getStart(), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, c1.getEnd());

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
        Chain c1 = new Chain("c1", "c1", app.getGroup("local"));
        app.addEventSource(c1);
        DTOState s = plan1.addState(c1, app.getGroup("local"));

        DTOState s1 = c1.addState(noop);
        DTOState s2 = c1.addState(failure);
        DTOState s3 = c1.addState(or);

        c1.connect(c1.getStart(), s1);
        c1.connect(c1.getStart(), s2);
        c1.connect(s1, s3);
        c1.connect(s2, s3);
        c1.connect(s3, c1.getEnd());

        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);

        // Analysis
        waitForEnded(6, 10, 500);
        checkHistory(5, 1);
    }
}
