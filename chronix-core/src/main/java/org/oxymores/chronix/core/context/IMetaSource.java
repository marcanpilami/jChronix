package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.UUID;

import org.osgi.framework.ServiceReference;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;

public interface IMetaSource
{
    public DTO getEventSource(UUID id);

    public <T extends DTO & Serializable> void registerSource(T source, EventSourceBehaviour service, String pluginName);

    public <T extends DTO> void unregisterSource(T source);
}
