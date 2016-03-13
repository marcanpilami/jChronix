package org.oxymores.chronix.core.source.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.dto.DTOPlaceGroup;

/**
 * A container is a special event source which contains a sub-part of the global plan. An example is a reusable "function" - a part of the
 * global plan that must be repeated many times. They can also be used to keep the plan organised.<br>
 * A container defines a scope - an isolated bubble from which events cannot leak. Scopes are not transitive: if a source contained by this
 * scope is itself a container, it is a different scope.<br>
 * In addition to the normal event source methods, it contains methods to allow the engine to access the sub-plan.
 */
public abstract class EventSourceContainer extends EventSourceTriggered
{
    private static final long serialVersionUID = -118374232425130974L;

    protected List<DTOState> states = new ArrayList<DTOState>();
    protected List<DTOTransition> transitions = new ArrayList<DTOTransition>();

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
    protected void checkCanAddSource(EventSource source)
    {
        if (this.id.equals(source.id))
        {
            throw new IllegalArgumentException("a container cannot contain itself");
        }
        if (source instanceof EventSourceOptionNoState)
        {
            throw new IllegalArgumentException("the nature of this source forbids it to be added to a container");
        }
    }

    protected DTOState addState(EventSource s, UUID runsOnId)
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

    public DTOState addState(EventSource s, DTOPlaceGroup runsOn)
    {
        return addState(s, runsOn.getId());
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
        DTOTransition tr = new DTOTransition();
        tr.setFrom(from.getId());
        tr.setTo(to.getId());
        tr.setGuard1(0);
        this.transitions.add(tr);
    }
}
