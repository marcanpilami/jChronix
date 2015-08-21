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
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.ApplicationPath;
import org.slf4j.Logger;
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
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *
 * @author Marc-Antoine
 */
@ApplicationPath("ws")
public class RestApplication extends ResourceConfig implements ServletContextListener
{
    private static final Logger log = LoggerFactory.getLogger(RestApplication.class);
    private ChronixContext ctx;
    private boolean closeOnExit = false;

    public RestApplication(@Context ServletContext context)
    {
        MDC.put("node", "webservice");
        log.info("Creating a new Chronix WS application");

        Object o = context.getAttribute("context");
        if (o == null)
        {
            // This happens during tests on a standard web server (a chronix engine would set the init params)
            // So create test data inside a test db.
            String dbPath = "C:\\TEMP\\db1";
            closeOnExit = true;
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

                String localNodeId = en1.getId().toString();

                ctx = new ChronixContext("simu", dbPath, true, dbPath + "\\hist.db", dbPath + "\\transac.db");
                ctx.setLocalNode(ctx.getNetwork().getNode(UUID.fromString(localNodeId)));

                try (org.sql2o.Connection conn = ctx.getHistoryDataSource().beginTransaction())
                {
                    RunLog l1 = new RunLog();
                    l1.setActiveNodeId(UUID.randomUUID());
                    l1.setApplicationId(UUID.randomUUID());
                    l1.setChainId(UUID.randomUUID());
                    l1.setChainLaunchId(UUID.randomUUID());
                    l1.setExecutionNodeId(UUID.randomUUID());
                    l1.setId(UUID.randomUUID());
                    l1.setPlaceId(UUID.randomUUID());
                    l1.setActiveNodeName("nodename");
                    l1.setApplicationName("appli");
                    l1.setBeganRunningAt(DateTime.now());
                    l1.setChainName("chain");
                    l1.setDns("localhost");
                    l1.setEnteredPipeAt(DateTime.now());
                    l1.setExecutionNodeName("nodename");
                    l1.setLastKnownStatus("OK");
                    l1.setLogPath("/ii/oo");
                    l1.setWhatWasRun("cmd1");
                    l1.setResultCode(0);
                    l1.setMarkedForUnAt(DateTime.now());

                    l1.insertOrUpdate(conn);
                    conn.commit();
                }
            }
            catch (ChronixPlanStorageException ex)
            {
                log.error("Failed to create test data", ex);
                return;
            }
        }
        else
        {
            ctx = (ChronixContext) o;
        }

        this.property(MarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);
        this.property(UnmarshallerProperties.JSON_WRAPPER_AS_ARRAY_NAME, true);

        this.register(new ServiceClient(ctx));
        this.register(new ServiceConsole(ctx));
        this.register(ErrorListener.class);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        log.debug("Servlet context is loading");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        log.info("Servlet context is closing");
        if (closeOnExit && this.ctx != null)
        {
            this.ctx.close();
            this.ctx = null;
        }
    }
}
