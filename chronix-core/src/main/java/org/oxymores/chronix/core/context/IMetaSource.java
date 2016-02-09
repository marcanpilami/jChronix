package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.UUID;

import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;

public interface IMetaSource
{
    public EventSource getEventSource(UUID id);

    public <T extends EventSource & Serializable> void registerSource(T source, String pluginSymbolicName);

    public <T extends EventSource> void unregisterSource(T source);
}
