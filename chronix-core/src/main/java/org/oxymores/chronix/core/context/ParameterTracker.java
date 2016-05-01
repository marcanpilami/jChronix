package org.oxymores.chronix.core.context;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.oxymores.chronix.core.ParameterHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ParameterTracker implements ServiceTrackerCustomizer<ParameterProvider, ParameterProvider>
{
    private static final Logger log = LoggerFactory.getLogger(ParameterTracker.class);
    private ChronixContextMeta ctx;
    private Bundle bd = FrameworkUtil.getBundle(ParameterTracker.class);

    ParameterTracker(ChronixContextMeta ctx)
    {
        this.ctx = ctx;
    }

    @Override
    public ParameterProvider addingService(ServiceReference<ParameterProvider> ref)
    {
        // get the service reference - it will stored alongside the parameter (if any)
        ParameterProvider srv = bd.getBundleContext().getService(ref);
        if (srv == null)
        {
            log.warn("Parameter plugin has disappeared before finishing its registration: " + ref.getClass().getCanonicalName());
            return null;
        }

        log.info(
                "Parameter plugin registering: " + srv.getClass().getCanonicalName() + " from bundle " + ref.getBundle().getSymbolicName());

        // Match the wrappers and the corresponding providers.
        List<Application> apps = new ArrayList<>(this.ctx.getApplications());
        apps.addAll(this.ctx.getDrafts());
        int i = 0;
        for (Application app : apps)
        {
            // TODO: shared parameters
            // for (DTOParameter prm : app.getO)

            for (EventSourceWrapper esw : app.getEventSourceWrappers().values())
            {
                for (ParameterHolder prm : esw.getParameters())
                {
                    if (prm.getProviderClassName() != null && prm.getProviderClassName().equals(srv.getClass().getCanonicalName()))
                    {
                        prm.setProvider(srv);
                        i++;
                    }
                }
            }
        }
        log.debug("Provider " + srv.getClass().getCanonicalName() + " is uesed by " + i
                + " distinct parameters in all applications including drafts.");

        return srv;
    }

    @Override
    public void modifiedService(ServiceReference<ParameterProvider> reference, ParameterProvider service)
    {
        // Nothing to do
    }

    @Override
    public void removedService(ServiceReference<ParameterProvider> ref, ParameterProvider service)
    {
        log.info("Parameter plugin is going away: " + ref.getClass().getCanonicalName() + ". It was from bundle "
                + ref.getBundle().getSymbolicName());

        // TODO: remove parameters.
    }

}
