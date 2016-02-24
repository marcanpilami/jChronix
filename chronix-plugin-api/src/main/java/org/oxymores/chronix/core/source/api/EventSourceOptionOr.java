package org.oxymores.chronix.core.source.api;

/**
 * By implementing this interface (which is empty) an {@link EventSource} signals the engine that States implementing this source can
 * receive multiple incoming transitions and that a logical OR should occur between these transitions (that is: the State will be triggered
 * each time a transition is made possible).<br>
 * Without implementing this interface or its sister {@link EventSourceOptionAnd} only one incoming transition is allowed.
 */
public interface EventSourceOptionOr
{

}
