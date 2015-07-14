/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.wapi;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

/**
 *
 * @author Marc-Antoine
 */
@ApplicationPath("ws")
public class RestApplication extends ResourceConfig
{
    public RestApplication(ChronixContext ctx)
    {
        //this.packages("org.oxymores.chronix.wapi");
        this.register(new ServiceClient(ctx));
    }

    public RestApplication()
    {
        ChronixContext ctx = null;
        String db1 = "C:\\TEMP\\db1";
        try
        {
            Network n = new Network();
            ExecutionNode en1 = PlanBuilder.buildExecutionNode(n, "e1", "localhost", 1789);
            ExecutionNode en2 = PlanBuilder.buildExecutionNode(n, "e2", "localhost", 1400);
            ExecutionNode en3 = PlanBuilder.buildExecutionNode(n, "e3", "localhost", 1804);
            en1.setConsole(true);
            en1.connectTo(en2, NodeConnectionMethod.TCP);
            en2.connectTo(en3, NodeConnectionMethod.RCTRL);

            Place p1 = PlanBuilder.buildPlace(n, "master node", "master node", en1);
            Place p2 = PlanBuilder.buildPlace(n, "second node", "second node", en2);
            Place p3 = PlanBuilder.buildPlace(n, "hosted node by second node", "third node", en3);

            ChronixContext.saveNetwork(n, new File(db1));

            Application a1 = DemoApplication.getNewDemoApplication();
            ChronixContext.saveApplicationAndMakeCurrent(a1, new File(db1));

            ctx = ChronixContext.loadContext(db1, null, null, "test", true, null, null);
        }
        catch (ChronixPlanStorageException ex)
        {
            Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, null, ex);
        }

        this.register(new ServiceClient(ctx));
    }
}
