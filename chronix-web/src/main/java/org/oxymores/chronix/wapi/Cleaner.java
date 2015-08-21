package org.oxymores.chronix.wapi;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 Small helper to close context at the destruction of the servlet context.
 Mostly usefull for debu environment with a lot of reloads, as this avoid
 CL leaks.

 May have to be merged into RestApplication.
 */
@WebListener
public class Cleaner implements ServletContextListener
{
    private static final Logger log = LoggerFactory.getLogger(Cleaner.class);

    @Override
    public void contextInitialized(ServletContextEvent sce)
    {
        log.debug("Servlet context is loading");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce)
    {
        log.info("Destroying REST application");
        if (RestApplication.ctx != null)
        {
            RestApplication.ctx.close();
            RestApplication.ctx = null;
        }
    }

}
