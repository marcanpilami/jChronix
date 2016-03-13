package org.oxymores.chronix.source.basic.reg;

import java.io.File;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;
import org.oxymores.chronix.source.basic.dto.Failure;

@Component(immediate = true, service = EventSourceProvider.class)
public class FailureBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "failure";
    }

    @Override
    public String getSourceDescription()
    {
        return "An event source that always fails. A helper for tests.";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry reg)
    {
        Failure n = new Failure();
        reg.registerSource(n);
    }
}
