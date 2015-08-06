/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.wapi;

import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
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

    public RestApplication(@Context ServletContext context)
    {
        ChronixContext ctx;

        String dbPath = context.getInitParameter("db_path");
        String localNodeId = context.getInitParameter("local_node_id");

        if (dbPath == null || localNodeId == null)
        {
            // This happens during tests on a standard web server (a chronix engine would set the init params)
            // So create test data inside a test db.
            dbPath = "C:\\TEMP\\db1";
            try
            {
                Network n = new Network();
                ExecutionNode en1 = PlanBuilder.buildExecutionNode(n, "e1", "localhost", 1789);
                en1.setX(100);
                en1.setY(100);
                ExecutionNode en2 = PlanBuilder.buildExecutionNode(n, "e2", "localhost", 1400);
                en2.setX(200);
                en2.setY(200);
                ExecutionNode en3 = PlanBuilder.buildExecutionNode(n, "e3", "localhost", 1804);
                en3.setX(300);
                en3.setY(300);
                en1.setConsole(true);
                en1.connectTo(en2, NodeConnectionMethod.TCP);
                en2.connectTo(en3, NodeConnectionMethod.RCTRL);

                Place p1 = PlanBuilder.buildPlace(n, "master node", en1);
                Place p2 = PlanBuilder.buildPlace(n, "second node", en2);
                Place p3 = PlanBuilder.buildPlace(n, "hosted node by second node", en3);

                ChronixContext.saveNetwork(n, new File(dbPath));

                Application a1 = DemoApplication.getNewDemoApplication();
                ChronixContext.saveApplicationAndMakeCurrent(a1, new File(dbPath));

                localNodeId = en1.getId().toString();
            }
            catch (ChronixPlanStorageException ex)
            {
                Logger.getLogger(RestApplication.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

        try
        {
            ctx = ChronixContext.loadContext(dbPath, null, null, "test", true, null, null);
            ctx.setLocalNode(ctx.getNetwork().getNode(UUID.fromString(localNodeId)));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return;
        }

        this.register(new ServiceClient(ctx));
    }
}
