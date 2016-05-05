package org.oxymores.chronix.source.chain.prv;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.source.DTOEvent;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.api.source.EventSourceField;
import org.oxymores.chronix.api.source.EventSourceProvider;

@Component
public class PlanProvider implements EventSourceProvider
{
    @Override
    public String getName()
    {
        return "plan";
    }

    @Override
    public String getDescription()
    {
        return "the base container of a production plan";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        return new ArrayList<>(0);
    }

    @Override
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event)
    {
        return true;
    }
}
