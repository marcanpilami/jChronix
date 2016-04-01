package org.oxymores.chronix.source.basic.prv;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRegistry;
import org.oxymores.chronix.source.basic.dto.Noop;

@Component(immediate = true, service = EventSourceProvider.class)
public class NoopBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "no-operation";
    }

    @Override
    public String getSourceDescription()
    {
        return "an event source that does nothing. Used as placeholder or for tests.";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry reg)
    {
        Noop n = new Noop();
        reg.registerSource(n);
    }
}
