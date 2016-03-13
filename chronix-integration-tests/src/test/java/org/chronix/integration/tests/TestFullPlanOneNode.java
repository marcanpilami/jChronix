package org.chronix.integration.tests;

import org.junit.Assert;
import org.junit.Test;
import org.oxymore.chronix.source.chain.dto.Chain;
import org.oxymores.chronix.agent.command.api.RunnerConstants;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.source.command.dto.ShellCommand;

public class TestFullPlanOneNode extends BaseIT
{
    @Test
    public void testPlan() throws InterruptedException
    {
        // Application content
        ShellCommand sc = new ShellCommand("c1", "c1", "echo aa", RunnerConstants.SHELL_WINCMD);
        app.addEventSource(sc);

        Chain c = new Chain("first chain", "integration test chain", app.getGroup("local"));
        app.addEventSource(c);
        DTOState n1 = c.addState(sc);
        c.connect(c.getStart(), n1);
        c.connect(n1, c.getEnd());

        DTOState s = plan1.addState(c, app.getGroup("local"));

        save();

        // Tests
        meta.resetCache();
        DTOApplication a2 = meta.getApplication(app.getId());
        Assert.assertEquals("test app", a2.getName());

        Assert.assertEquals(9, a2.getEventSources().size());
        boolean found = false;
        for (EventSource d : a2.getEventSources())
        {
            if (d instanceof Chain && "first chain".equals(((Chain) d).getName()))
            {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        addAndStartEngine("local");
        order.orderLaunch(a2.getId(), s.getId(), envt.getPlace("local").getId(), true);

        waitForOk(4, 10);
        checkHistory(4, 0);
    }
}
