package org.oxymore.chronix.chain.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymore.chronix.source.ChainBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.EventSourceTriggered;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOChain extends EventSourceContainer
{
    private static final long serialVersionUID = -5008049889665310849L;

    private List<DTOState> states = new ArrayList<DTOState>();
    private List<DTOTransition> transitions = new ArrayList<DTOTransition>();

    public DTOChain()
    {
        // For serialisation
    }

    /**
     * Helper constructor that creates a ready to use chain.
     */
    public DTOChain(String name, String description, DTOPlaceGroup runsOn)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();

        DTOState s1 = new DTOState();
        s1.setEventSourceId(DTOChainStart.START_ID);
        s1.setX(50);
        s1.setY(50);
        s1.setRunsOnId(runsOn.getId());

        DTOState s2 = new DTOState();
        s2.setEventSourceId(DTOChainEnd.END_ID);
        s2.setX(50);
        s2.setY(200);
        s2.setRunsOnId(runsOn.getId());

        this.states.add(s1);
        this.states.add(s2);
    }

    /**
     * Creates a ready to use plan (a chain without start or end that cannot be a subscope)
     * 
     * @param name
     * @param description
     */
    public DTOChain(String name, String description)
    {

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

    public DTOState addState(EventSource s, DTOPlaceGroup runsOn)
    {
        return addState(s, runsOn.getId());
    }

    private DTOState addState(EventSource s, UUID runsOnId)
    {
        if (!(s instanceof EventSourceTriggered))
        {
            throw new IllegalArgumentException("a chain cannot contain sources that create their own scope");
        }

        DTOState s1 = new DTOState();
        s1.setEventSourceId(s.getId());
        s1.setX(50);
        s1.setY(50);
        s1.setRunsOnId(runsOnId);
        this.states.add(s1);
        return s1;
    }

    public void connect(DTOState from, DTOState to)
    {
        if (!this.states.contains(from) || !this.states.contains(to))
        {
            throw new IllegalArgumentException("cannot connect states which do not belong to the chain");
        }
        DTOTransition tr = new DTOTransition();
        tr.setFrom(from.getId());
        tr.setTo(to.getId());
        tr.setGuard1(0);
        this.transitions.add(tr);
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
            if (s.getEventSourceId().equals(DTOChainStart.START_ID))
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
            if (s.getEventSourceId().equals(DTOChainEnd.END_ID))
            {
                return s;
            }
        }
        throw new RuntimeException("chain has no end");
    }

    // Stupid GET/SET
    public List<DTOState> getStates()
    {
        return states;
    }

    void setStates(List<DTOState> states)
    {
        this.states = states;
    }

    public List<DTOTransition> getTransitions()
    {
        return transitions;
    }

    void setTransitions(List<DTOTransition> transitions)
    {
        this.transitions = transitions;
    }

    @Override
    public List<DTOState> getContainedStates()
    {
        return this.states;
    }

    @Override
    public List<DTOTransition> getContainedTransitions()
    {
        return this.transitions;
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ChainBehaviour.class;
    }
}
