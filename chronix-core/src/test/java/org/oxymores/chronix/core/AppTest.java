package org.oxymores.chronix.core;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

/**
 * Unit test for simple App.
 */
public class AppTest
{
    private static Logger log = Logger.getLogger(AppTest.class);
    private static Validator validator;

    @BeforeClass
    public static void setUp()
    {
        validator = ChronixContext.getValidator();
    }

    @Test
    public void testBasicValidation()
    {
        ShellCommand sc = new ShellCommand();
        sc.setDescription("houba hop");

        Set<ConstraintViolation<ShellCommand>> v = validator.validate(sc);
        for (ConstraintViolation<ShellCommand> violation : v)
        {
            log.debug(violation.getLeafBean() + " - " + violation.getPropertyPath() + " - " + violation.getMessage());
        }
        Assert.assertEquals(3, v.size());
    }

    @Test
    public void testChainEndsValidation()
    {
        Application a = new Application();
        a.setname("test");
        PlaceGroup pg1 = PlanBuilder.buildDefaultLocalNetwork(a);

        log.debug("** Validating an incorrect chain");
        Chain c = new Chain();
        c.setDescription("houba hop");
        c.setName("rr");
        a.addActiveElement(c);

        Set<ConstraintViolation<Chain>> v = validator.validate(c);
        for (ConstraintViolation<Chain> violation : v)
        {
            log.debug(violation.getMessage());
        }
        Assert.assertEquals(2, v.size());

        log.debug("** Validating a correct chain");
        c = PlanBuilder.buildChain(a, "test", "description", pg1);
        v = validator.validate(c);
        for (ConstraintViolation<Chain> violation : v)
        {
            log.debug(violation.getMessage());
        }
        Assert.assertEquals(0, v.size());
    }

    @Test
    public void testChainCycleValidation()
    {
        Application a = PlanBuilder.buildApplication("test app", "description");
        PlaceGroup pg1 = PlanBuilder.buildDefaultLocalNetwork(a);

        log.debug("** Validating a cyclic chain");
        Chain c = PlanBuilder.buildChain(a, "test", "description", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo ee", "cmd1", "test cmd");
        State s1 = PlanBuilder.buildState(c, pg1, sc1);
        State s2 = PlanBuilder.buildState(c, pg1, sc1);

        c.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(c.getEndState());
        s2.connectTo(s1); // Cycle!

        Set<ConstraintViolation<Chain>> v = validator.validate(c);
        for (ConstraintViolation<Chain> violation : v)
        {
            log.debug(violation.getMessage());
        }
        Assert.assertEquals(1, v.size());

        log.debug("** Validating a correct chain");
        c = PlanBuilder.buildChain(a, "test", "description", pg1);
        c.getStartState().connectTo(c.getEndState());
        v = validator.validate(c);
        for (ConstraintViolation<Chain> violation : v)
        {
            log.debug(violation.getMessage());
        }
        Assert.assertEquals(0, v.size());
    }

    @Test
    public void testAppGlobalValidation()
    {
        log.debug("** Validating an app with a cyclic chain");
        Application a = PlanBuilder.buildApplication("test app", "description");
        PlaceGroup pg1 = PlanBuilder.buildDefaultLocalNetwork(a);

        Chain c = PlanBuilder.buildChain(a, "test", "description", pg1);
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo ee", "cmd1", "test cmd");
        State s1 = PlanBuilder.buildState(c, pg1, sc1);
        State s2 = PlanBuilder.buildState(c, pg1, sc1);

        c.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s2.connectTo(c.getEndState());
        s2.connectTo(s1); // Cycle!

        Set<ConstraintViolation<Application>> v = validator.validate(a);
        for (ConstraintViolation<Application> violation : v)
        {
            log.debug(violation.getLeafBean() + " - " + violation.getPropertyPath() + " - " + violation.getMessage());
        }
        Assert.assertEquals(1, v.size());
    }

    @Test
    public void testDemoApp()
    {
        Application a = DemoApplication.getNewDemoApplication();
        Set<ConstraintViolation<Application>> v = validator.validate(a);
        for (ConstraintViolation<Application> violation : v)
        {
            log.debug(violation.getLeafBean() + " - " + violation.getPropertyPath() + " - " + violation.getMessage());
        }
        Assert.assertEquals(0, v.size());
    }
}
