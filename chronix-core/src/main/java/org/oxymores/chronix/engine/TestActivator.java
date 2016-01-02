package org.oxymores.chronix.engine;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.engine.helpers.SenderHelpers;

/**
 * TEST CLASS - to be removed before release.
 *
 */
public class TestActivator implements BundleActivator
{
    ChronixEngine e;

    @Override
    public void start(BundleContext context) throws Exception
    {
        e = new ChronixEngine("C:\\TEMP\\db1", "local");
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
    public void stop(BundleContext context) throws Exception
    {
        e.stopEngine();
        e.waitForStopEnd();
    }

}
