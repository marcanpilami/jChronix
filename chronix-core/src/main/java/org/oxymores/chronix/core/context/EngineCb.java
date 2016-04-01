package org.oxymores.chronix.core.context;

import java.util.UUID;

import org.oxymores.chronix.api.source.EventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRegistry;

public class EngineCb implements EventSourceRegistry
{
    private IMetaSource metaSource;
    private EventSourceProvider service;
    private String pluginName;

    public EngineCb(IMetaSource metaSource, EventSourceProvider service, String pluginName)
    {
        this.metaSource = metaSource;
        this.service = service;
        this.pluginName = pluginName;
    }

    @Override
    public EventSource getEventSource(UUID id)
    {
        return this.metaSource.getEventSource(id);
    }

    @Override
    public <T extends EventSource> void registerSource(T source)
    {
        this.metaSource.registerSource(source, pluginName);
    }

    @Override
    public <T extends EventSource> void unregisterSource(T source)
    {
        this.metaSource.unregisterSource(source);
    }
}
