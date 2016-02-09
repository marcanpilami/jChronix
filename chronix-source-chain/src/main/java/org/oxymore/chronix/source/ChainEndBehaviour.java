package org.oxymore.chronix.source;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymore.chronix.chain.dto.DTOChainEnd;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceRegistry;

@Component(immediate = true, service = EventSourceBehaviour.class)
public class ChainEndBehaviour extends EventSourceBehaviour
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
    public void deserialize(File sourceFile, EventSourceRegistry cb)
    {
        // Only one chain end for the whole application. No need to put it inside a file.
        DTOChainEnd end = new DTOChainEnd();
        cb.registerSource(end);
    }

    @Override
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        List<Class<? extends EventSource>> res = new ArrayList<>();
        res.add(DTOChainEnd.class);
        return res;
    }
}
