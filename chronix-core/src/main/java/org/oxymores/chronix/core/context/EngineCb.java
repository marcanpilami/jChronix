package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.lang.NotImplementedException;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;

public class EngineCb implements EngineCallback
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
    public void sendMessageXCXXXXX(Object msg, String destinationQueue)
    {
        throw new NotImplementedException();
    }

    @Override
    public void sendRunResult(EventSourceRunResult r)
    {
        throw new NotImplementedException();
    }

    @Override
    public DTO getEventSource(UUID id)
    {
        return this.metaSource.getEventSource(id);
    }

    @Override
    public <T extends DTO & Serializable> void registerSource(T source)
    {
        this.metaSource.registerSource(source, service, pluginName);
    }

    @Override
    public <T extends DTO> void unregisterSource(T source)
    {
        this.metaSource.unregisterSource(source);
    }
}
