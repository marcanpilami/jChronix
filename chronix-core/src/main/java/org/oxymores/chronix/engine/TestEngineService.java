package org.oxymores.chronix.engine;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.engine.api.ChronixEngine;

@Component
public class TestEngineService implements ChronixEngine
{
    org.oxymores.chronix.engine.ChronixEngine e;

    @Override
    public void start()
    {
        e = new org.oxymores.chronix.engine.ChronixEngine("C:\\TEMP\\db1", "local", "C:\\TEMP\\db1");
        try
        {
            e.start();
            e.waitForInitEnd();
        }
        catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        e.stopEngine();
        e.waitForStopEnd();
    }
}
