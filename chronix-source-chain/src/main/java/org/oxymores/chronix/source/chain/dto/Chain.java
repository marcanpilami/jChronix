package org.oxymores.chronix.source.chain.dto;

import java.util.UUID;

import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSource;
import org.oxymores.chronix.api.source.EventSourceContainer;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.EventSourceTriggered;
import org.oxymores.chronix.api.source.JobDescription;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.source.chain.prv.ChainBehaviour;

public class Chain extends EventSourceContainer
{
    private static final long serialVersionUID = -5008049889665310849L;

    protected Chain()
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
