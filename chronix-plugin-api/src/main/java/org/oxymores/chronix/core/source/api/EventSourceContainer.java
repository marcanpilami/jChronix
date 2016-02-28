package org.oxymores.chronix.core.source.api;

import java.util.List;

/**
 * A container is a special event source which contains a sub-part of the global plan. An example is a reusable "function" - a part of the
 * global plan that must be repeated many times. They can also be used to keep the plan organized.<br>
 * A container defines a scope - an isolated bubble from which events cannot leak. Scopes are not transitive: if a source contained by this
 * scope is itself a container, it is a different scope.<br>
 * In addition to the normal event source methods, it contains methods to allow the engine to access the sub-plan.
 */
public abstract class EventSourceContainer extends EventSourceTriggered
{
    private static final long serialVersionUID = -118374232425130974L;

    /**
     * This method returns all the states directly inside the scope defined by the source (i.e. not in sub-scopes).<br>
     * Returning null is an error. Empty lists are OK.
     */
    public abstract List<DTOState> getContainedStates();

    /**
     * All the transitions contained by the scope.<br>
     * Returning null is an error. Empty lists are OK.
     */
    public abstract List<DTOTransition> getContainedTransitions();
}
