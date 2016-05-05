package org.oxymores.chronix.api.source;

/**
 * By implementing this interface (which is empty) an {@link EventSource} signals the engine that States implementing this source can
 * receive multiple incoming transitions and that a logical OR should occur between these transitions (that is: the State will be triggered
 * each time an incoming transition is made possible).<br>
 * Without implementing this interface or its sister {@link OptionAnd} only one incoming transition is allowed.
 */
public interface OptionOr
{

}
