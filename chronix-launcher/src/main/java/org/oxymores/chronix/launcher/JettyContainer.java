/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.oxymores.chronix.launcher;

import java.io.File;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

public class JettyContainer
{
    private static final Logger log = Logger.getLogger(JettyContainer.class);

    private Server server;

    public JettyContainer(String dbPath, String localNodeId, int port)
    {
        log.info("Starting web server on port " + port);
        log.info("The web server will use plain HTTP for all communications (no SSL)");

        server = new Server(port);

        // There are two places where the web service might be: inside ./www (nominal) or ../web-service/target/chronix-web-* (tests)
        File f = new File("./www");
        if (!f.canRead())
        {
            f = new File("../chronix-web/target/");
            for (String sf : f.list())
            {
                if (sf.startsWith("chronix-web"))
                {
                    f = new File(FilenameUtils.concat(f.getAbsolutePath(), sf));
                    break;
                }
            }
        }
        if (!f.canRead())
        {
            log.error("Web server was asked to start but could not find the web service. It won't start as a consequence");
            return;
        }

        log.info("Web server will load application at " + f.getAbsolutePath());
        WebAppContext ctx = new WebAppContext(f.getAbsolutePath(), "/");
        ctx.setInitParameter("db_path", dbPath);
        ctx.setInitParameter("local_node_id", localNodeId);
        ctx.setLogUrlOnStart(true);

        ctx.setConfigurations(new Configuration[]
        {
            new WebInfConfiguration(), new WebXmlConfiguration(), new AnnotationConfiguration()
        });
        server.setHandler(ctx);

        try
        {
            server.start();
            while (!ctx.isStarted())
            {
                Thread.sleep(100);
            }
        }
        catch (Exception ex)
        {
            log.error("Could not start web server. Scheduler is still OK.", ex);
        }

        log.info("The web server has finished booting");
    }

    void stop()
    {
        log.info("The web server will stop");
        try
        {
            server.stop();
            server.join();
            server.destroy();
            log.info("The web server has stopped");
        }
        catch (Exception e)
        {
            log.warn("An exception occurred during web server shutdown.", e);
        }
    }

}
