package org.chronix.integration.tests;

import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;

/**
 * Single node, multiple places per group.
 */
public class TestParallelismSingleNode extends BaseIT
{
    @Test
    public void testP1()
    {
        // P1. Two places P1 & P2 on single node.
        // START (P1) -> FAIL ON P1 (all places //) -> NOOP (all places //) -> END (P1)

        DTOPlaceGroup pgLocal = app.getGroup("local");

        // Network & deployment
        DTOPlaceGroup pg1 = new DTOPlaceGroup("pg1", "pg1");
        DTOPlaceGroup pg2 = new DTOPlaceGroup("pg2", "pg2");
        DTOPlaceGroup pgAll = new DTOPlaceGroup("pgAll", "pgAll");
        app.addGroup(pg1).addGroup(pg2).addGroup(pgAll);

        DTOPlace p1 = new DTOPlace("p1", envt.getExecutionNode("local")).addMemberOfGroup(pg1, pgAll);
        DTOPlace p2 = new DTOPlace("p2", envt.getExecutionNode("local")).addMemberOfGroup(pg2, pgAll);
        envt.addPlace(p1).addPlace(p2);

        // Payload
        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, app, "chain1", "integration test chain", null)
                .setAllStates(pgLocal);
        DTOEventSource fail = new DTOEventSource(failOnPlacePrv, app, "fail1", "fail1").setField("PLACENAME", "p1");

        DTOState s1 = chain1.addState(fail, pgAll).setParallel(true);
        DTOState s2 = chain1.addState(noop, pgAll).setParallel(true);

        chain1.connect(getChainStart(chain1), s1);
        chain1.connect(s1, s2);
        chain1.connect(s2, getChainEnd(chain1));

        // Done
        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), getChainStart(chain1).getId(), envt.getPlace("local").getId(), true);

        // Tests: NOOP should have run on only one place, and the END should not have run at all.
        waitForEnded(4, 10, 200);
        checkHistory(3, 1);
    }
}
