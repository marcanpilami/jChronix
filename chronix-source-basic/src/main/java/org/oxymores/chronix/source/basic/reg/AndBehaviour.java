package org.oxymores.chronix.source.basic.reg;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.basic.dto.And;

@Component(immediate = true, service = EventSourceProvider.class)
public class AndBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "and";
    }

    @Override
    public String getSourceDescription()
    {
        return "an event source that does nothing by itself - it is simply a logical door that does an AND between all incoming transitions.";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry reg)
    {
        And n = new And();
        reg.registerSource(n);
    }
}
