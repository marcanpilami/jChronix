package org.oxymore.chronix.source.chain.reg;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.source.chain.dto.Noop;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;

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
