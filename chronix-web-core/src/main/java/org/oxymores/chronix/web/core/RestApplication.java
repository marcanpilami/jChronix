package org.oxymores.chronix.web.core;

import org.glassfish.jersey.servlet.ServletContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = {})
public class RestApplication
{
    private static final Logger log = LoggerFactory.getLogger(RestApplication.class);

    private final String API_CONTEXT = "/ws";

    private HttpService httpService;
    private ServletContainer jerseyContainer;
    ComponentContext ctx;

    private ServiceTracker<RestServiceApi, RestServiceApi> serviceTracker;

    @Reference
    public void bindHttpService(HttpService service)
    {
        this.httpService = service;

    }

    public void unbindHttpService(HttpService service)
    {
        this.httpService = null;
    }

    @Activate
    public void activate(ComponentContext ctx)
    {
        this.ctx = ctx;
        this.jerseyContainer = new ServletContainer(new RestApplicationConfiguration());
        try
        {
            this.httpService.registerServlet(API_CONTEXT, jerseyContainer, null, null);
        }
        catch (Exception e)
        {
            log.error("Could not register the REST API inside the OSGI web container", e);
        }
        log.info("REST API was registered inside the OSGI web container");

        serviceTracker = new ServiceTracker<>(this.ctx.getBundleContext(), RestServiceApi.class, new RestServiceTrackerCustomizer(this));
        this.serviceTracker.open();
        log.info("REST API host has begun looking for services");
    }

    @Deactivate
    public void deactivate()
    {
        this.serviceTracker.close();
        log.info("REST API host has stopped looking for services");

        this.httpService.unregister(API_CONTEXT);
        log.info("REST API was unregistered from the OSGI web container");
    }

    void resync(RestServiceApi add)
    {
        RestApplicationConfiguration conf = new RestApplicationConfiguration();
        Object[] svcs = serviceTracker.getServices();
        if (svcs != null)
        {
            for (Object pi : svcs)
            {
                if (pi != null)
                {
                    RestServiceApi api = (RestServiceApi) pi;
                    conf.register(api);
                }
            }
        }
        if (add != null)
        {
            conf.register(add);
        }

        this.jerseyContainer.reload(conf);
        log.info("REST API host has reloaded its configuration");
    }
}
