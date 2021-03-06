package org.chronix.integration.tests;

import org.chronix.integration.helpers.BaseIT;
import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOFunctionalSequence;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class TestCalendarSingleNode extends BaseIT
{
    @Test
    public void testCalendar()
    {
        // Independent: S0 : NOOP OC (cal1)
        // chain1: START -> S1 NOOP (cal1 + 0) -> S2 NOOP (cal1 - 1) -> END
        // launched twice.
        DTOPlaceGroup pgLocal = app.getGroup("local");
        DTOEventSourceContainer c1 = new DTOEventSourceContainer(chainPrv, app, "c1", "integration test chain", null).setAllStates(pgLocal);

        DTOFunctionalSequence seq = new DTOFunctionalSequence("simple sequence", "for tests").addOccurrence("O1").addOccurrence("O2")
                .addOccurrence("O3").addOccurrence("O4").addOccurrence("O5").addOccurrence("O6").addOccurrence("O7").addOccurrence("O9")
                .addOccurrence("O9").addOccurrence("10");
        app.addSequence(seq);

        DTOState s = plan1.addState(c1, pgLocal);
        DTOState s1 = c1.addState(noop, pgLocal).setCalendar(seq);
        DTOState s2 = c1.addState(noop, pgLocal).setCalendar(seq).setCalendarShift(-1);
        DTOState s0 = plan1.addState(noop, pgLocal).setCalendar(seq).setMoveCalendarForwardOnSuccess(true);

        c1.connect(getChainStart(c1), s1);
        c1.connect(s1, s2);
        c1.connect(s2, getChainEnd(c1));

        save();

        // Two launches - second should block
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);
        waitForOk(5, 10, 200); // 5 is: the chain itself + its four states.
        checkHistory(5, 0);

        order.orderLaunch(app.getId(), s.getId(), envt.getPlace("local").getId(), true);
        waitForOk(6, 10, 400);
        checkHistory(6, 0);

        // Advance the calendar
        order.orderLaunch(app.getId(), s0.getId(), envt.getPlace("local").getId(), true);
        waitForOk(11, 10, 200);
        checkHistory(11, 0);
    }

    @Test
    public void testCalendarConstrainedTransition()
    {
        DTOPlaceGroup pgLocal = app.getGroup("local");

        DTOFunctionalSequence seq = new DTOFunctionalSequence("simple sequence", "for tests").addOccurrence("01").addOccurrence("02")
                .addOccurrence("03").addOccurrence("04").addOccurrence("05").addOccurrence("06").addOccurrence("07").addOccurrence("09")
                .addOccurrence("09").addOccurrence("10");
        app.addSequence(seq);

        // Plan1 : External ---cal1---> Noop1 (cal1)
        // External is triggered with data corresponding to the first calendar occurrence.

        DTOEventSource external1 = new DTOEventSource(extPrv, app, "external1", "external1").setField("regularExpression", "^(.*)$");
        app.addEventSource(external1);

        DTOState external1_state1 = plan1.addState(external1, pgLocal).setCalendar(seq);
        DTOState noop_state1 = plan1.addState(noop, pgLocal).setCalendar(seq);

        plan1.connect(external1_state1, noop_state1).setCalendarAware(true);

        // Start
        save();
        addAndStartEngine("local");

        // First launch - external with 01 as data - respects transition condition so should go to the end.
        order.orderExternal(external1.getName(), "01");

        waitForOk(2, 10);
        checkHistory(2, 0);

        // Send the same external event a second time - the second node should not fire.
        order.orderExternal(external1.getName(), "01");

        waitForOk(3, 10, 500);
        checkHistory(3, 0);
    }
}
