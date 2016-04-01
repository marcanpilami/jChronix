package org.oxymores.chronix.api.source;

/**
 * By implementing this interface (which is empty) an {@link EventSource} signals the engine that States implementing this source can
 * receive multiple incoming transitions and that a logical AND should occur between these transitions (that is: the State will be triggered
 * only when all incoming transitions are made possible).<br>
 * Without implementing this interface or its sister {@link EventSourceOptionOr} only one incoming transition is allowed.
 */
public interface EventSourceOptionAnd
{

}
