package org.oxymores.chronix.engine;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "scheduler", configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
public class EngineFactoryService implements ChronixEngine
{
    private static final Logger log = LoggerFactory.getLogger(EngineFactoryService.class);

    private org.oxymores.chronix.engine.ChronixEngine e;
    private String name;
    private Map<String, String> configuration;

    @Activate
    public synchronized void activate(Map<String, String> config)
    {
        String name = config.getOrDefault("chronix.cluster.node.name", "local");
        log.info("OSGI service holding node " + name + " is activating");
        this.name = name;
        this.configuration = config;
        start();
    }

    @Deactivate
    public synchronized void deactivate(ComponentContext ctx)
    {
        log.info("OSGI service holding node " + name + " was asked to stop");
        stop();
    }

    @Modified
    public synchronized void reload(Map<String, String> config)
    {
        this.name = config.getOrDefault("chronix.cluster.node.name", "local");
        this.configuration = config;
        log.info("OSGI service holding engine " + name + " was modified and will reload");
        e.queueReloadConfiguration();
    }

    @Override
    public synchronized void start()
    {
        String nodeName = configuration.getOrDefault("chronix.cluster.node.name", "local");
        String metabase = configuration.getOrDefault("chronix.repository.path", "./metabase");
        e = new org.oxymores.chronix.engine.ChronixEngine(metabase, nodeName, metabase);
        try
        {
            e.start();
            // e.waitForInitEnd();
        }
        catch (Exception e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

    @Override
    public synchronized void stop()
    {
        if (e != null && e.shouldRun())
        {
            e.stopEngine();
            e.waitForStopEnd();
        }
    }

    @Override
    public synchronized void waitOperational()
    {
        e.waitForInitEnd();
    }

    @Override
    public synchronized void waitShutdown()
    {
        e.waitForStopEnd();
    }
}