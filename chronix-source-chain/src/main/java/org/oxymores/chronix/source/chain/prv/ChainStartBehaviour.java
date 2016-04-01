package org.oxymores.chronix.source.chain.prv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.EventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRegistry;
import org.oxymores.chronix.source.chain.dto.ChainStart;

@Component(immediate = true, service = EventSourceProvider.class)
public class ChainStartBehaviour extends EventSourceProvider
{
    @Override
    public String getSourceName()
    {
        return "chain start";
    }

    @Override
    public String getSourceDescription()
    {
        return "the first item launched when a chain is launched. The first event in a chain claunch is always created by this.";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry cb)
    {
        // Only one chain start for the whole application. No need to put it inside a file.
        ChainStart start = new ChainStart();
        cb.registerSource(start);
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(ChainStart.class);
        return res;
    }
}
