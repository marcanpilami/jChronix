package org.oxymores.chronix.engine;

import java.util.List;
import javax.jms.JMSException;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestEnvVarAndParams extends TestBase
{
    @Test
    public void testEnvVar() throws JMSException
    {
        // 1 - Public env var is present in shell session (state level)
        // 2 - Public env var is present in shell session (shell command level)
        // 3 - Env var with addToRunEnvironment = false (private) is not present
        // 4 - Overloaded public env var is present with overloaded value
        // 5 - Env var set by a shell command is propagated to following commands
        // 6 - Public env var is propagated throughout the chain
        // 7 - Public env var overloaded by private one is not present

        Application a = PlanBuilder.buildApplication("test", db1);
        PlaceGroup pg1 = PlanBuilder.buildPlaceGroup(a, "singlenode", "singlenode", n.getPlacesList().toArray(new Place[0]));
        Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pg1);

        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo %myvar1%", "case 1", "");
        State s1 = PlanBuilder.buildState(c, pg1, sc1);
        s1.addEnvVar("myvar1", "houba");
        c.getStartState().connectTo(s1);

        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo %myvar2%", "case 2", "");
        sc2.addEnvVar("myvar2", "pouet");
        State s2 = PlanBuilder.buildState(c, pg1, sc2);
        s1.connectTo(s2);

        ShellCommand sc3 = PlanBuilder.buildShellCommand(a, "echo %myvar3%", "case 3", "");
        sc3.addInternalEnvVar("myvar3", "pouet");
        State s3 = PlanBuilder.buildState(c, pg1, sc3);
        s2.connectTo(s3);

        ShellCommand sc4 = PlanBuilder.buildShellCommand(a, "echo %myvar4%", "case 4", "");
        sc4.addEnvVar("myvar4", "pouet");
        State s4 = PlanBuilder.buildState(c, pg1, sc4);
        s4.addEnvVar("myvar4", "hop");
        s3.connectTo(s4);

        ShellCommand sc5_1 = PlanBuilder.buildShellCommand(a, "echo set SUPERVAR=123", "case 5 1of2", "");
        State s5_1 = PlanBuilder.buildState(c, pg1, sc5_1);
        s4.connectTo(s5_1);
        ShellCommand sc5_2 = PlanBuilder.buildShellCommand(a, "echo %SUPERVAR%", "case 5 2of2", "");
        State s5_2 = PlanBuilder.buildState(c, pg1, sc5_2);
        s5_1.connectTo(s5_2);

        ShellCommand sc6 = PlanBuilder.buildShellCommand(a, "echo %myvar1%", "case 6", "");
        State s6 = PlanBuilder.buildState(c, pg1, sc6);
        s5_2.connectTo(s6);

        ShellCommand sc7 = PlanBuilder.buildShellCommand(a, "echo %myvar7%", "case 7", "");
        sc7.addEnvVar("myvar7", "erk");
        State s7 = PlanBuilder.buildState(c, pg1, sc7);
        s7.addInternalEnvVar("myvar7", "ouf");
        s6.connectTo(s7);

        s7.connectTo(c.getEndState());

        // GO
        addApplicationToDb(db1, a);
        ChronixEngine e = addEngine(db1, "local");
        startEngines();

        SenderHelpers.runStateInsidePlan(c.getStartState(), e.getContext());
        List<RunLog> res = LogHelpers.waitForHistoryCount(e.getContext(), 10);

        Assert.assertEquals(10, res.size());
        Assert.assertEquals("houba", res.get(1).getShortLog());
        Assert.assertEquals("pouet", res.get(2).getShortLog());
        Assert.assertEquals("%myvar3%", res.get(3).getShortLog());
        Assert.assertEquals("hop", res.get(4).getShortLog());
        Assert.assertEquals("123", res.get(6).getShortLog());
        Assert.assertEquals("houba", res.get(7).getShortLog());
        Assert.assertEquals("%myvar7%", res.get(8).getShortLog());
    }

    @Test
    public void testEnvVarInParameter()
    {
        // Parameter without env var is present
        // Parameter with env var is present (even if addToRunEnvironment = false)
        // Same with overlaoded env var.
    }

}
