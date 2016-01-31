package org.oxymores.chronix.core.context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EventSourceTracker implements ServiceTrackerCustomizer<EventSourceBehaviour, EventSourceBehaviour>
{
    private static final Logger log = LoggerFactory.getLogger(EventSourceTracker.class);
    private ChronixContextMeta ctx;
    private Bundle bd = FrameworkUtil.getBundle(EventSourceTracker.class);

    EventSourceTracker(ChronixContextMeta ctx)
    {
        this.ctx = ctx;
    }

    @Override
    public EventSourceBehaviour addingService(ServiceReference<EventSourceBehaviour> ref)
    {
        // On add, simply init the plugin.
        log.info("Event source plugin registering: " + ref.toString() + " from bundle " + ref.getBundle().getSymbolicName());

        // get the service reference - it will stored alongside the event sources (if any)
        EventSourceBehaviour srv = bd.getBundleContext().getService(ref);
        if (srv == null)
        {
            log.warn("Event source plugin has disappeared before finishing its registration: " + ref.getClass().getCanonicalName());
            return null;
        }

        // Each application may have data created by this plugin - load that data
        Collection<Application2> apps = new ArrayList<>();
        apps.addAll(this.ctx.getApplications());
        apps.addAll(this.ctx.getDrafts());
        for (Application2 app : apps)
        {
            File appDir = this.ctx.getRootApplication(app.getId());
            if (!appDir.isDirectory())
            {
                throw new ChronixInitializationException("Configuration directory " + appDir.getAbsolutePath() + " cannot be opened");
            }

            File bundleDir = new File(FilenameUtils.concat(appDir.getAbsolutePath(), ref.getBundle().getSymbolicName()));
            if (!bundleDir.isDirectory() && !bundleDir.mkdir())
            {
                throw new ChronixInitializationException(
                        "Configuration directory " + bundleDir.getAbsolutePath() + " does not exist and could not be created");
            }

            log.info("Asking plugin " + ref.getBundle().getSymbolicName() + " to read directory " + bundleDir.getAbsolutePath());
            srv.deserialize(bundleDir, new EngineCb(app, srv));
        }

        return srv;
    }

    @Override
    public void modifiedService(ServiceReference<EventSourceBehaviour> reference, EventSourceBehaviour service)
    {
        // Nothing to do
    }

    @Override
    public void removedService(ServiceReference<EventSourceBehaviour> ref, EventSourceBehaviour service)
    {
        log.info("Source event plugin is going away: " + ref.getClass().getCanonicalName() + ". It was from bundle "
                + ref.getBundle().getSymbolicName());
        for (Class<? extends DTO> klass : service.getExposedDtoClasses())
        {
            for (Application2 app : this.ctx.getApplications())
            {
                for (Object o : app.getEventSources(klass))
                {
                    app.unregisterSource(klass.cast(o));
                }
            }
        }
    }

}
