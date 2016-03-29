package org.oxymores.chronix.core.context.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.core.engine.api.ChronixEngine;

// Disabled for now. Not sure if really useful.
/**
 * This service waits for engines to start and adds API services (configurations with no auto start) for each engine.
 */
// @Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class EngineLeech
{
    private ServiceTracker<ChronixEngine, ChronixEngine> tracker;
    private Map<String, Configuration> createdHistConfigurations = new HashMap<>();
    private Map<String, Configuration> createdOrderConfigurations = new HashMap<>();
    private Map<String, Configuration> createdMetaConfigurations = new HashMap<>();

    //////////////////////////////////////
    // OSGI magic fields
    ConfigurationAdmin confSrv;

    @Reference
    void setConfService(ConfigurationAdmin c)
    {
        this.confSrv = c;
    }

    //
    ///////////////////////////////////////

    @Activate
    public void startup(final BundleContext ctx)
    {
        tracker = new ServiceTracker<>(ctx, ChronixEngine.class, new ServiceTrackerCustomizer<ChronixEngine, ChronixEngine>()
        {
            @Override
            public ChronixEngine addingService(ServiceReference<ChronixEngine> reference)
            {
                // Get node name
                String name = (String) (reference.getProperty("chronix.cluster.node.name") == null ? "local"
                        : reference.getProperty("chronix.cluster.node.name"));

                // Add a configuration with the same node name
                try
                {
                    Configuration cfg = confSrv.createFactoryConfiguration("org.oxymores.chronix.core.engine.api.HistoryService", null);
                    Dictionary<String, Object> props = cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties();
                    for (String key : reference.getPropertyKeys())
                    {
                        if (!key.startsWith("chronix."))
                        {
                            continue;
                        }
                        props.put(key, reference.getProperty(key));
                    }
                    cfg.update(props);
                    createdHistConfigurations.put(name, cfg);

                    cfg = confSrv.createFactoryConfiguration("org.oxymores.chronix.core.engine.api.OrderService", null);
                    props = cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties();
                    for (String key : reference.getPropertyKeys())
                    {
                        if (!key.startsWith("chronix."))
                        {
                            continue;
                        }
                        props.put(key, reference.getProperty(key));
                    }
                    cfg.update(props);
                    createdOrderConfigurations.put(name, cfg);

                    cfg = confSrv.createFactoryConfiguration("org.oxymores.chronix.core.engine.api.PlanAccessService", null);
                    props = cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties();
                    for (String key : reference.getPropertyKeys())
                    {
                        if (!key.startsWith("chronix."))
                        {
                            continue;
                        }
                        props.put(key, reference.getProperty(key));
                    }
                    cfg.update(props);
                    createdMetaConfigurations.put(name, cfg);
                }
                catch (IOException e)
                {
                    // log.error("Could not create a command runner agent. Commands (shell, ...) will not run!", e);
                }

                // Just return the service
                return ctx.getService(reference);
            }

            @Override
            public void modifiedService(ServiceReference<ChronixEngine> reference, ChronixEngine service)
            {
                // Nothing for now
            }

            @Override
            public void removedService(ServiceReference<ChronixEngine> reference, ChronixEngine service)
            {
                // Get node name
                String name = (String) (reference.getProperty("chronix.cluster.node.name") == null ? "local"
                        : reference.getProperty("chronix.cluster.node.name"));

                try
                {
                    createdHistConfigurations.get(name).delete();
                    createdHistConfigurations.remove(name);

                    createdOrderConfigurations.get(name).delete();
                    createdOrderConfigurations.remove(name);

                    createdMetaConfigurations.get(name).delete();
                    createdMetaConfigurations.remove(name);
                }
                catch (Exception e)
                {
                    // log.error("Could not remove an agent. Too much RAM may be used due to this", e);
                }
            }
        });

        tracker.open();
    }

    public void close()
    {
        this.tracker.close();

        for (Configuration c : this.createdHistConfigurations.values())
        {
            try
            {
                c.delete();
            }
            catch (IOException e)
            {
                // log.error("Could not remove an agent. Too much RAM may be used due to this", e);
            }
        }
        this.createdHistConfigurations.clear();

        for (Configuration c : this.createdOrderConfigurations.values())
        {
            try
            {
                c.delete();
            }
            catch (IOException e)
            {
                // log.error("Could not remove an agent. Too much RAM may be used due to this", e);
            }
        }
        this.createdOrderConfigurations.clear();

        for (Configuration c : this.createdMetaConfigurations.values())
        {
            try
            {
                c.delete();
            }
            catch (IOException e)
            {
                // log.error("Could not remove an agent. Too much RAM may be used due to this", e);
            }
        }
        this.createdMetaConfigurations.clear();
    }
}
