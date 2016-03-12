package org.oxymore.chronix.chain.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymore.chronix.source.ChainBehaviour;
import org.oxymore.chronix.source.PlanBehaviour;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOPlan extends EventSourceContainer
{
    private static final long serialVersionUID = -5008049889665310849L;

    private List<DTOState> states = new ArrayList<DTOState>();
    private List<DTOTransition> transitions = new ArrayList<DTOTransition>();

    public DTOPlan()
    {
        // For serialisation
    }

    /**
     * Helper constructor that creates a ready to use plan.
     */
    public DTOPlan(String name, String description)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUN
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        throw new IllegalStateException("A plan itself cannot be launched. It contains elemnts that launch themselves");
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPERS
    ///////////////////////////////////////////////////////////////////////////

    public void addTransition(DTOTransition tr)
    {
        this.transitions.add(tr);
    }

    public DTOState addState(EventSource s, DTOPlaceGroup runsOn)
    {
        return addState(s, runsOn.getId());
    }

    private DTOState addState(EventSource s, UUID runsOnId)
    {
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
        return PlanBehaviour.class;
    }
}
