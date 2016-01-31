package org.oxymores.chronix.engine;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

@Component
public class TestEngineService implements ChronixEngine
{
    org.oxymores.chronix.engine.ChronixEngine e;

    @Override
    public void start()
    {
        e = new org.oxymores.chronix.engine.ChronixEngine("C:\\TEMP\\db1", "local");
        try
        {
            e.start();
            e.waitForInitEnd();

            // Corresponds to testChainLaunch unit test application
            State s = e.getContext().getApplicationByName("test1").getChain("simple chain").getStartState();
            SenderHelpers.runStateInsidePlan(s, e.getContext());
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
