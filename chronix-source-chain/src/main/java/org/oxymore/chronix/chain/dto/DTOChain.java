package org.oxymore.chronix.chain.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;

public class DTOChain implements DTO, Serializable
{
    private static final long serialVersionUID = -5008049889665310849L;

    private String name;
    private String description;
    private UUID id;

    private List<DTOState> states = new ArrayList<DTOState>();
    private List<DTOTransition> transitions = new ArrayList<DTOTransition>();

    public DTOChain()
    {
        // For serialisation
    }

    /**
     * Helper constructor that creates a ready to use chain.
     */
    public DTOChain(String name, String description)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();

        DTOState s1 = new DTOState();
        s1.setEventSourceId(new DTOChainStart().getId());
        s1.setX(50);
        s1.setY(50);

        DTOState s2 = new DTOState();
        s1.setEventSourceId(new DTOChainStart().getId());
        s1.setX(50);
        s1.setY(200);

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

}
