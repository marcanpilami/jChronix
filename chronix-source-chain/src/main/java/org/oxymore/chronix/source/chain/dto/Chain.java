package org.oxymore.chronix.source.chain.dto;

import java.util.UUID;

import org.oxymore.chronix.source.chain.reg.ChainBehaviour;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class Chain extends EventSourceContainer
{
    private static final long serialVersionUID = -5008049889665310849L;

    public Chain()
    {
        // For serialisation
    }

    /**
     * Helper constructor that creates a ready to use chain.
     */
    public Chain(String name, String description, DTOPlaceGroup runsOn)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();

        DTOState s1 = new DTOState();
        s1.setEventSourceId(ChainStart.START_ID);
        s1.setX(50);
        s1.setY(50);
        s1.setRunsOnId(runsOn.getId());

        DTOState s2 = new DTOState();
        s2.setEventSourceId(ChainEnd.END_ID);
        s2.setX(50);
        s2.setY(200);
        s2.setRunsOnId(runsOn.getId());

        this.states.add(s1);
        this.states.add(s2);
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUN
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        // Enqueue start (in a new context)
        cb.launchState(this.getStart());

        // A chain only starts its own start event source. The actual RunResult is sent by the end source, so all we need here is to return
        // at once.
        return null;
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPERS
    ///////////////////////////////////////////////////////////////////////////

    public DTOState addState(EventSource s)
    {
        return addState(s, this.getStart().getRunsOnId());
    }

    @Override
    protected void checkCanAddSource(EventSource source)
    {
        if (!(source instanceof EventSourceTriggered))
        {
            throw new IllegalArgumentException("a chain cannot contain sources that create their own scope");
        }
        super.checkCanAddSource(source);
    }

    public void setPlaceGroup(DTOPlaceGroup group)
    {
        this.getStart().setRunsOnId(group.getId());
        this.getEnd().setRunsOnId(group.getId());
    }

    public DTOState getStart()
    {
        for (DTOState s : this.states)
        {
            if (s.getEventSourceId().equals(ChainStart.START_ID))
            {
                return s;
            }
        }
        throw new RuntimeException("chain has no start");
    }

    public DTOState getEnd()
    {
        for (DTOState s : this.states)
        {
            if (s.getEventSourceId().equals(ChainEnd.END_ID))
            {
                return s;
            }
        }
        throw new RuntimeException("chain has no end");
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ChainBehaviour.class;
    }
}
