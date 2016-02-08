package org.oxymores.chronix.core.source.api;

import java.util.List;

/**
 * A container is a special event source which contains a sub-part of the global plan. An example is a reusable "function".<br>
 * A container defines a scope - an isolated bubble from which events cannot leak. Scopes are not transitive: if a source contained by this
 * scope is itself a container, it is a different scope.<br>
 * In addition to the normal event source methods, it also contains methods to allow the engine to access the sub-plan.
 */
public interface DTOContainer extends EventSource
{
    /**
     * This method returns all the states directly inside the scope defined by the source (i.e. not in sub-scopes).<br>
     * Returning null is an error. Empty lists are OK.
     */
    public List<DTOState> getContainedStates();

    /**
     * All the transitions contained by the scope.<br>
     * Returning null is an error. Empty lists are OK.
     */
    public List<DTOTransition> getContainedTransitions();
}
