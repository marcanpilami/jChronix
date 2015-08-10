/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.wapi;

import java.io.File;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import java.util.Date;
import java.util.Random;
import javax.persistence.EntityManager;
import javax.ws.rs.ApplicationPath;
import org.apache.log4j.Logger;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.eclipse.persistence.jaxb.UnmarshallerProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.timedata.RunLog;
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
    private static final Logger log = Logger.getLogger(RestApplication.class);
    public static ChronixContext ctx;

    public RestApplication(@Context ServletContext context)
    {
        log.info("Creating a new Chronix WS application");

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
                log.fatal("", ex);
                return;
            }
        }

        try
        {
            ctx = new ChronixContext("simu", dbPath, "TransacUnit", "HistoryUnit", true, dbPath + "\\hist.db", dbPath + "\\transac.db");
            ctx.setLocalNode(ctx.getNetwork().getNode(UUID.fromString(localNodeId)));
        }
        catch (Exception e)
        {
            log.fatal("", e);
            return;
        }

        try
        {
            EntityManager em = ctx.getHistoryEM();
            RunLog l1 = new RunLog();
            l1.setActiveNodeId("123");
            l1.setApplicationId("123");
            l1.setChainId("123");
            l1.setChainLaunchId("123");
            l1.setExecutionNodeId("123");
            Random gen = new Random(DateTime.now().getMillis());
            l1.setId(((Integer) gen.nextInt()).toString());
            l1.setPlaceId("123");
            l1.setActiveNodeName("nodename");
            l1.setApplicationName("appli");
            l1.setBeganRunningAt(new Date());
            l1.setChainName("chain");
            l1.setDns("localhost");
            l1.setEnteredPipeAt(new Date());
            l1.setExecutionNodeName("nodename");
            l1.setLastKnownStatus("OK");
            l1.setLogPath("/ii/oo");
            l1.setWhatWasRun("cmd1");
            l1.setResultCode(0);
            l1.setMarkedForUnAt(new Date());

            em.getTransaction().begin();
            em.persist(l1);
            em.getTransaction().commit();
            em.close();
        }
        catch (Exception e)
        {
            log.error("", e);
            return;
        }

        this.property(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
        this.property(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

        this.register(new ServiceClient(ctx));
        this.register(new ServiceConsole(ctx));
        this.register(ErrorListener.class);
    }
}
