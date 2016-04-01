package org.oxymores.chronix.source.chain.prv;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.EventSource;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRegistry;
import org.oxymores.chronix.source.chain.dto.ChainEnd;

@Component(immediate = true, service = EventSourceProvider.class)
public class ChainEndBehaviour extends EventSourceProvider
{

    @Override
    public String getSourceName()
    {
        return "CHAIN END";
    }

    @Override
    public String getSourceDescription()
    {
        return "A chain must always end by running this source";
    }

    @Override
    public void deserialise(File sourceFile, EventSourceRegistry cb)
    {
        // Only one chain end for the whole application. No need to put it inside a file.
        ChainEnd end = new ChainEnd();
        cb.registerSource(end);
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(ChainEnd.class);
        return res;
    }
}
