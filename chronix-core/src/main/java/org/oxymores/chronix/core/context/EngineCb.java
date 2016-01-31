package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.lang.NotImplementedException;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.engine.modularity.runner.RunResult;

public class EngineCb implements EngineCallback
{
    private IMetaSource metaSource;
    private EventSourceBehaviour service;

    public EngineCb(IMetaSource metaSource, EventSourceBehaviour service)
    {
        this.metaSource = metaSource;
        this.service = service;
    }

    @Override
    public void sendMessageXCXXXXX(Object msg, String destinationQueue)
    {
        throw new NotImplementedException();
    }

    @Override
    public void sendRunResult(RunResult r)
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
        this.metaSource.registerSource(source, service);
    }

    @Override
    public <T extends DTO> void unregisterSource(T source)
    {
        this.metaSource.unregisterSource(source);
    }
}
