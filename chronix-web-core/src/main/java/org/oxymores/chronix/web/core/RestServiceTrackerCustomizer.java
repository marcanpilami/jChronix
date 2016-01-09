package org.oxymores.chronix.web.core;

import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestServiceTrackerCustomizer implements ServiceTrackerCustomizer<RestServiceApi, RestServiceApi>
{
    private static final Logger log = LoggerFactory.getLogger(RestApplication.class); // On purpose
    private RestApplication parent;

    RestServiceTrackerCustomizer(RestApplication sc)
    {
        this.parent = sc;
    }

    @Override
    public synchronized RestServiceApi addingService(ServiceReference<RestServiceApi> reference)
    {
        RestServiceApi svc = this.parent.ctx.getBundleContext().getService(reference);
        // ResourceConfig cfg = this.parent.jerseyContainer.getConfiguration(); // .register(svc);
        this.parent.resync(svc);
        log.info("REST API host has registered a new service named " + svc.getClass().getName());
        return svc;
    }

    @Override
    public synchronized void modifiedService(ServiceReference<RestServiceApi> reference, RestServiceApi service)
    {
        this.parent.resync(null);
        log.info("REST API host has registered a service change");
    }

    @Override
    public synchronized void removedService(ServiceReference<RestServiceApi> reference, RestServiceApi service)
    {
        this.parent.resync(null);
        log.info("REST API host has unregistered a service");
    }
}
