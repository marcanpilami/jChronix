package org.oxymores.chronix.core;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

public class TestContext
{
    @Test
    public void saveAndLoadApp() throws IOException, ChronixPlanStorageException
    {
        File ctx = new File("C:\\TEMP\\db1");
        FileUtils.cleanDirectory(ctx);

        Application a1 = org.oxymores.chronix.planbuilder.DemoApplication.getNewDemoApplication("marsu", 1234);
        ChronixContext.saveApplicationAndMakeCurrent(a1, ctx);

        ChronixContext context = ChronixContext.loadContext(ctx.getAbsolutePath(), "TransacUnit", "HistoryUnit", "marsu:1234", false, "C:\\TEMP\\db1\\jpa1", "C:\\TEMP\\db1\\jpa2");
        Application a = context.getApplicationByName("Demo");
        Assert.assertEquals("test application auto created", a.getDescription());
    }
}
