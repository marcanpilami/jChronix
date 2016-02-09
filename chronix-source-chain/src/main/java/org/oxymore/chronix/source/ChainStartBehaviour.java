package org.oxymore.chronix.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChainStart;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;

@Component(immediate = true, service = EventSourceBehaviour.class)
public class ChainStartBehaviour extends EventSourceBehaviour
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
    public void deserialize(File sourceFile, EventSourceRegistry cb)
    {
        // Only one chain start for the whole application. No need to put it inside a file.
        DTOChainStart start = new DTOChainStart();
        cb.registerSource(start);
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(DTOChainStart.class);
        return res;
    }
}
