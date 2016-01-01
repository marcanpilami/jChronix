package org.oxymores.chronix.engine;

import java.io.FileWriter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class TestActivator implements BundleActivator
{
    ChronixEngine e;

    @Override
    public void start(BundleContext context) throws Exception
    {
        e = new ChronixEngine("C:\\TEMP\\db1", "local");
        try
        {
            FileWriter f = new FileWriter("C:\\TEMP\\db1\\marsu.txt");
            f.write("lllllllllll");
            f.close();
            e.startEngine(false);
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
    }

}
