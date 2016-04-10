package org.oxymores.chronix.core.context;

import java.util.ArrayList;
import java.util.Collection;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventSourceTracker implements ServiceTrackerCustomizer<EventSourceProvider, EventSourceProvider>
{
    private static final Logger log = LoggerFactory.getLogger(EventSourceTracker.class);
    private ChronixContextMeta ctx;
    private Bundle bd = FrameworkUtil.getBundle(EventSourceTracker.class);

    EventSourceTracker(ChronixContextMeta ctx)
    {
        this.ctx = ctx;
    }

    @Override
    public EventSourceProvider addingService(ServiceReference<EventSourceProvider> ref)
    {
        // On add, simply init the plugin.

        // get the service reference - it will stored alongside the event sources (if any)
        EventSourceProvider srv = bd.getBundleContext().getService(ref);
        if (srv == null)
        {
            log.warn("Event source plugin has disappeared before finishing its registration: " + ref.getClass().getCanonicalName());
            return null;
        }

        log.info("Event source plugin registering: " + srv.getClass().getCanonicalName() + " from bundle "
                + ref.getBundle().getSymbolicName());

        // Each application may have data created by this plugin - load that data
        Collection<Application> apps = new ArrayList<>();
        apps.addAll(this.ctx.getApplications());
        apps.addAll(this.ctx.getDrafts());
        for (Application app : apps)
        {
            int i = 0;
            for (EventSourceWrapper esw : app.getEventSourceWrappers().values())
            {
                if (esw.getPluginSymbolicName().equals(ref.getBundle().getSymbolicName())
                        && esw.getPluginClassName().equals(srv.getClass().getCanonicalName()))
                {
                    esw.setProvider(srv);
                    i++;
                }
            }

            log.trace("Plugin " + ref.getBundle().getSymbolicName() + " has " + i + " event sources in application " + app.getName());
        }

        return srv;
    }

    @Override
    public void modifiedService(ServiceReference<EventSourceProvider> reference, EventSourceProvider service)
    {
        // Nothing to do
    }

    @Override
    public void removedService(ServiceReference<EventSourceProvider> ref, EventSourceProvider service)
    {
        log.info("Source event plugin is going away: " + ref.getClass().getCanonicalName() + ". It was from bundle "
                + ref.getBundle().getSymbolicName());

        for (Application app : this.ctx.getApplications())
        {
            for (DTOEventSource o : app.getEventSources(service))
            {
                app.removeSource(o);
            }
        }
    }

}
