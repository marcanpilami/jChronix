package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.List;

public class DTOChain
{
    private String name;
    private String description;
    private String id;

    private List<DTOState> states = new ArrayList<DTOState>();
    private List<DTOTransition> transitions = new ArrayList<DTOTransition>();

    public void addTransition(DTOTransition tr)
    {
        this.transitions.add(tr);
    }

    public void addState(DTOState s)
    {
        this.states.add(s);
    }
    
    // Stupid GET/SET
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

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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
