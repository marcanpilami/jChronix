package org.oxymores.chronix.api.source;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.dto.DTOPlaceGroup;

public class DTOEventSourceContainer extends DTOEventSource
{
    private static final long serialVersionUID = -8557075941985914540L;

    protected List<DTOState> states = new ArrayList<DTOState>(20);
    protected List<DTOTransition> transitions = new ArrayList<DTOTransition>(20);

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    public DTOEventSourceContainer(EventSourceProvider factory, DTOApplication app, String name, String description, UUID id)
    {
        // Note: we call init and not super... because super must be first be fields init are done after super. Thanks Java.
        init(factory, app, name, description, id);
    }

    ///////////////////////////////////////////////////////////////////////////
    // State manipulation
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This method returns all the states directly inside the scope defined by the source (i.e. not in sub-scopes).<br>
     * This should always return a copy of the list, not the original list.<br>
     * Never returns null. Empty lists are OK.
     */
    public List<DTOState> getContainedStates()
    {
        return new ArrayList<>(states);
    }

    /**
     * Throws a IllegalArgumentException if the given source is not allowed to be added to this container.<br>
     * Default implementation allows every event source to be added except the source itself.
     */
    protected void checkCanAddSource(DTOEventSource source)
    {
        if (this.id.equals(source.id))
        {
            throw new IllegalArgumentException("a container cannot contain itself");
        }
        if (source.getBehaviour() instanceof OptionNoState)
        {
            throw new IllegalArgumentException("the nature of this source forbids it to be added to a container");
        }
    }

    protected DTOState addState(DTOEventSource s, UUID runsOnId)
    {
        checkCanAddSource(s);

        DTOState s1 = new DTOState();
        s1.setEventSourceId(s.getId());
        s1.setX(50);
        s1.setY(50);
        s1.setRunsOnId(runsOnId);
        this.states.add(s1);
        return s1;
    }

    public DTOState addState(DTOEventSource s, DTOPlaceGroup runsOn)
    {
        return addState(s, runsOn.getId());
    }

    public DTOEventSourceContainer setAllStates(DTOPlaceGroup group)
    {
        for (DTOState s : this.states)
        {
            s.setRunsOnId(group.getId());
        }
        return this;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Transition manipulation
    ///////////////////////////////////////////////////////////////////////////

    /**
     * All the transitions contained by the scope.<br>
     * This should always return a copy of the list, not the original list.<br>
     * Never returns null. Empty lists are OK.
     */
    public List<DTOTransition> getContainedTransitions()
    {
        return new ArrayList<>(transitions);
    }

    /**
     * Create a transition between two states. The states must already exist and be members of the container.
     */
    public void connect(DTOState from, DTOState to)
    {
        if (!this.states.contains(from) || !this.states.contains(to))
        {
            throw new IllegalArgumentException("cannot connect states which do not belong to the same container");
        }
        // TODO: add checks for CannotEmit/CannotReceive.

        DTOTransition tr = new DTOTransition();
        tr.setFrom(from.getId());
        tr.setTo(to.getId());
        tr.setGuard1(0);
        this.transitions.add(tr);
    }
}
