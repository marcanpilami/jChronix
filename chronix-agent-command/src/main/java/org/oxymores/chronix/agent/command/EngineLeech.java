package org.oxymores.chronix.agent.command;

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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service waits for engines to start and adds an agent for each engine.
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class EngineLeech
{
    private static final Logger log = LoggerFactory.getLogger(EngineLeech.class);

    private ServiceTracker<ChronixEngine, ChronixEngine> tracker;
    private Map<String, Configuration> createdConfigurations = new HashMap<>();

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
                    Configuration cfg = confSrv.createFactoryConfiguration("agent", null); // confSrv.getConfiguration("agent-" + name);
                    Dictionary<String, Object> props = cfg.getProperties();
                    if (props == null)
                    {
                        props = new Hashtable<String, Object>();
                    }

                    for (String key : reference.getPropertyKeys())
                    {
                        if (!key.startsWith("chronix."))
                        {
                            continue;
                        }
                        props.put(key, reference.getProperty(key));
                    }

                    log.info("Registering a new runner agent configuration for engine node " + name);
                    cfg.update(props);
                    createdConfigurations.put(name, cfg);
                }
                catch (IOException e)
                {
                    log.error("Could not create a command runner agent. Commands (shell, ...) will not run!", e);
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
                    createdConfigurations.get(name).delete();
                    createdConfigurations.remove(name);
                    log.info("Removing configuration for engine command runner agent " + name);
                }
                catch (Exception e)
                {
                    log.error("Could not remove an agent. Too much RAM may be used due to this", e);
                }
            }
        });

        tracker.open();
    }

    public void close()
    {
        this.tracker.close();

        for (Configuration c : this.createdConfigurations.values())
        {
            try
            {
                c.delete();
            }
            catch (IOException e)
            {
                log.error("Could not remove an agent. Too much RAM may be used due to this", e);
            }
        }
        this.createdConfigurations.clear();
    }
}
