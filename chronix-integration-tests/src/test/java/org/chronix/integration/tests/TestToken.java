package org.chronix.integration.tests;

import java.util.List;

import org.chronix.integration.helpers.BaseIT;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.core.engine.api.DTOToken;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.HistoryQuery;

public class TestToken extends BaseIT
{
    @Test
    public void testTokenSingleNode() throws InterruptedException
    {
        DTOPlaceGroup pgLocal = app.getGroup("local");

        // We check that states using the same token (with 1 token in pool) are serialised.
        // Chain1: Start -> sleep1 (token1)--> AND -> End
        // ..............-> sleep2 (token1)-|

        DTOToken token1 = new DTOToken("token1", "token1");
        app.addToken(token1);

        ///////////
        // Chain1
        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, app, "first chain", "integration test chain", null)
                .setAllStates(pgLocal);
        app.addEventSource(chain1);

        DTOEventSource sleep1 = new DTOEventSource(sleepPrv, app, "sleep1", "sleep1").setField("DURATION", "2");
        DTOEventSource sleep2 = new DTOEventSource(sleepPrv, app, "sleep2", "sleep2").setField("DURATION", "2");

        DTOState sleep1_state = chain1.addState(sleep1, pgLocal).addToken(token1);
        DTOState sleep2_state = chain1.addState(sleep2, pgLocal).addToken(token1);
        DTOState and_state = chain1.addState(and, pgLocal);
        chain1.connect(getChainStart(chain1), sleep1_state);
        chain1.connect(getChainStart(chain1), sleep2_state);
        chain1.connect(sleep1_state, and_state);
        chain1.connect(sleep2_state, and_state);
        chain1.connect(and_state, getChainEnd(chain1));

        // Meta is done.
        save();

        // Launch (from external event)
        addAndStartEngine("local");
        order.orderLaunch(app.getId(), getChainStart(chain1).getId(), envt.getPlace("local").getId(), true);

        // Global test
        waitForOk(5, 10);
        checkHistory(5, 0);

        // Launches serialised?
        HistoryQuery q = new HistoryQuery();
        q.setResultCode(0);
        DTORunLog s1 = null, s2 = null;
        List<DTORunLog> res = history.query(q).getRes();
        for (DTORunLog l : res)
        {
            if (l.getActiveNodeName().equals("sleep1"))
            {
                s1 = l;
            }
            if (l.getActiveNodeName().equals("sleep2"))
            {
                s2 = l;
            }
        }
        if (!(s1.getStoppedRunningAt().before(s2.getBeganRunningAt()) || s2.getStoppedRunningAt().before(s1.getBeganRunningAt())))
        {
            Assert.fail("launches were not serialized");
        }
    }
}
