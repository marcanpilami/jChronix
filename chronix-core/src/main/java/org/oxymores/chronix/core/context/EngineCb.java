package org.oxymores.chronix.core.context;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;

public class EngineCb implements EventSourceRegistry
{
    private IMetaSource metaSource;
    private EventSourceBehaviour service;
    private String pluginName;

    public EngineCb(IMetaSource metaSource, EventSourceBehaviour service, String pluginName)
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
