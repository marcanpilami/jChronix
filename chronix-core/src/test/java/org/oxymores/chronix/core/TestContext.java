package org.oxymores.chronix.core;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.PlanBuilder;

public class TestContext
{
    File ctx;

    @Before
    public void before() throws ChronixPlanStorageException
    {
        ctx = new File("C:\\TEMP\\db1");
        try
        {
            FileUtils.cleanDirectory(ctx);
        }
        catch (IOException e)
        {
        }
        Network n = PlanBuilder.buildLocalDnsNetwork();
        ChronixContext.saveNetwork(n, ctx);
    }

    @Test
    public void saveAndLoadApp() throws ChronixPlanStorageException
    {
        Application a1 = org.oxymores.chronix.planbuilder.DemoApplication.getNewDemoApplication("marsu", 1234);
        ChronixContext.saveApplication(a1, ctx);

        ChronixContext context = new ChronixContext("local", ctx.getAbsolutePath(), false, "C:\\TEMP\\db1\\jpa1", "C:\\TEMP\\db1\\jpa2");
        Application a = context.getApplicationByName("Demo");
        Assert.assertEquals("test application auto created", a.getDescription());

        context.close();
    }

    @Test
    public void versioning() throws Exception
    {
        Application a1 = org.oxymores.chronix.planbuilder.DemoApplication.getNewDemoApplication("marsu", 1234);
        ChronixContext.saveApplication(a1, ctx);

        ChronixContext context = new ChronixContext("local", ctx.getAbsolutePath(), false, "C:\\TEMP\\db1\\jpa1", "C:\\TEMP\\db1\\jpa2");
        Application a = context.getApplicationByName("Demo");
        Assert.assertEquals("test application auto created", a.getDescription());
        Assert.assertEquals(1, a.getVersion());
        Assert.assertTrue(a.isFromCurrentFile());

        // Modify a field => version should go up.
        a.setDescription("pppp");
        context.saveApplication(a);

        // Reload application to simulate engine restart...
        context = new ChronixContext("local", ctx.getAbsolutePath(), false, "C:\\TEMP\\db1\\jpa1", "C:\\TEMP\\db1\\jpa2");
        a = context.getApplicationByName("Demo");
        Assert.assertEquals("pppp", a.getDescription());
        Assert.assertEquals(2, a.getVersion());

        // This time save twice => version should only go up by 1.
        a.setDescription("gggg1");
        context.saveApplication(a);
        a.setDescription("gggg");
        context.saveApplication(a);
        context = new ChronixContext("local", ctx.getAbsolutePath(), false, "C:\\TEMP\\db1\\jpa1", "C:\\TEMP\\db1\\jpa2");
        a = context.getApplicationByName("Demo");
        Assert.assertEquals("gggg", a.getDescription());
        Assert.assertEquals(3, a.getVersion());

        context.close();
    }
}
