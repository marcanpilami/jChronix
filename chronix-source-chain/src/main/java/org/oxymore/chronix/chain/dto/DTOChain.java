package org.oxymore.chronix.chain.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTOContainer;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOChain implements DTOContainer
{
    private static final long serialVersionUID = -5008049889665310849L;

    private String name;
    private String description;
    private UUID id;
    private boolean enabled = true;

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

        this.addState(s1);
        this.addState(s2);
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPERS
    ///////////////////////////////////////////////////////////////////////////

    public void addTransition(DTOTransition tr)
    {
        this.transitions.add(tr);
    }

    public void addState(DTOState s)
    {
        this.states.add(s);
    }

    public void connect(DTOState from, DTOState to)
    {
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
            if (s.getEventSourceId() == DTOChainStart.START_ID)
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
            if (s.getEventSourceId() == DTOChainEnd.END_ID)
            {
                return s;
            }
        }
        throw new RuntimeException("chain has no end");
    }

    // Stupid GET/SET
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

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
    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

}
