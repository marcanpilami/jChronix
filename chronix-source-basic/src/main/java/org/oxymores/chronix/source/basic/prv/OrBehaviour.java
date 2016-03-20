package org.oxymores.chronix.source.basic.prv;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.basic.dto.Or;

@Component(immediate = true, service = EventSourceProvider.class)
public class OrBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "or";
    }

    @Override
    public String getSourceDescription()
    {
        return "an event source that does nothing by itself - it is simply a logical door that does an OR between all incoming transitions.";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry reg)
    {
        Or n = new Or();
        reg.registerSource(n);
    }
}
