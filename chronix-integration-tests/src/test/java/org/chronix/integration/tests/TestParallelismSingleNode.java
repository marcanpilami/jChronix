package org.chronix.integration.tests;

import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.HistoryQuery;

/**
 * Single node, multiple places per group.<br>
 * The name of the tests relates to the terminology used inside the plan development documentation.
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

        HistoryQuery q = new HistoryQuery();
        q.setResultCode(1);
        history.query(q);
        DTORunLog failedLog = null;
        for (DTORunLog l : q.getRes())
        {
            if (l.getPlaceName().equals("p1"))
            {
                failedLog = l;
                break;
            }
        }

        // Now force OK, the chain should end.
        order.orderForceOK(failedLog.getId());
        waitForEnded(6, 10, 200);
        checkHistory(5, 1);
    }

    @Test
    public void testP2()
    {
        // P2. Two places P1 & P2 on single node. We check we are in a mixed // case.
        // START (P1) -> S1: FAIL ON P1 (all places //) -> S3: AND (all places //) -> S4: NOOP (P2) -> END (P1)
        // ..........|-> S2: NOOP ............. (P2 //) |

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
                .setAllStates(pg1);
        DTOEventSource failOnP1 = new DTOEventSource(failOnPlacePrv, app, "failOnP1", "fail1").setField("PLACENAME", "p1");

        DTOState s1 = chain1.addState(failOnP1, pgAll).setParallel(true);
        DTOState s2 = chain1.addState(noop, pg2).setParallel(true);
        DTOState s3 = chain1.addState(and, pgAll).setParallel(true);
        DTOState s4 = chain1.addState(noop, pg2).setParallel(true);

        chain1.connect(getChainStart(chain1), s1);
        chain1.connect(getChainStart(chain1), s2);
        chain1.connect(s2, s3);
        chain1.connect(s1, s3);
        chain1.connect(s3, s4);
        chain1.connect(s4, getChainEnd(chain1));

        // Done
        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), getChainStart(chain1).getId(), p1.getId(), true);

        // Tests: AND should have run only on P2. (START OK on P1, S1 OK on P2, S1 failed on P1, NOOP OK on P2, AND OK on P2)
        waitForEnded(5, 10, 200);
        checkHistory(4, 1);

        HistoryQuery q = new HistoryQuery();
        q.setResultCode(1);
        history.query(q);
        DTORunLog failedLog = null;
        for (DTORunLog l : q.getRes())
        {
            if (l.getPlaceName().equals("p1"))
            {
                failedLog = l;
                break;
            }
        }

        // Now force OK, the chain should end.
        order.orderForceOK(failedLog.getId());
        waitForEnded(8, 10, 200);
        checkHistory(7, 1);
    }

    @Test
    public void testP5()
    {
        // P5 tests that // is triggered only when same group used on both ends of a transition.
        // START (P1) -> S1: FAIL ON P1 (P1 P2 //) -> S2: NOOP (P2 P3 //) -> END (P1)

        // Network & deployment
        DTOPlaceGroup pgP1 = new DTOPlaceGroup("pgP1", "pgP1");
        DTOPlaceGroup pgP1P2 = new DTOPlaceGroup("pgP1P2", "pgP1P2");
        DTOPlaceGroup pgP2P3 = new DTOPlaceGroup("pgP2P3", "pgP2P3");
        app.addGroup(pgP1).addGroup(pgP1P2).addGroup(pgP2P3);

        DTOPlace p1 = new DTOPlace("p1", envt.getExecutionNode("local")).addMemberOfGroup(pgP1, pgP1P2);
        DTOPlace p2 = new DTOPlace("p2", envt.getExecutionNode("local")).addMemberOfGroup(pgP1P2, pgP2P3);
        DTOPlace p3 = new DTOPlace("p3", envt.getExecutionNode("local")).addMemberOfGroup(pgP2P3);
        envt.addPlace(p1).addPlace(p2).addPlace(p3);

        // Payload
        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, app, "chain1", "integration test chain", null)
                .setAllStates(pgP1);
        DTOEventSource failOnP1 = new DTOEventSource(failOnPlacePrv, app, "failOnP1", "fail1").setField("PLACENAME", "p1");

        DTOState s1 = chain1.addState(failOnP1, pgP1P2).setParallel(true);
        DTOState s2 = chain1.addState(noop, pgP2P3).setParallel(true);

        chain1.connect(getChainStart(chain1), s1);
        chain1.connect(s1, s2);
        chain1.connect(s2, getChainEnd(chain1));

        // Done
        save();

        // GO
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), getChainStart(chain1).getId(), p1.getId(), true);

        // Tests: S2 should not have run anywhere.
        waitForEnded(3, 10, 500);
        checkHistory(2, 1);

        HistoryQuery q = new HistoryQuery();
        q.setResultCode(1);
        history.query(q);
        DTORunLog failedLog = null;
        for (DTORunLog l : q.getRes())
        {
            if (l.getPlaceName().equals("p1"))
            {
                failedLog = l;
                break;
            }
        }

        // Now force OK, the chain should end.
        order.orderForceOK(failedLog.getId());
        waitForEnded(6, 10, 200);
        checkHistory(5, 1);
    }
}
