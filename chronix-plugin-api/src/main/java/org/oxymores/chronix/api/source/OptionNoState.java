package org.oxymores.chronix.api.source;

/**
 * By implementing this interface (which is empty) an {@link EventSource} signals the engine that no state can use this source. This is used
 * by event sources that are actually self-contained, such as plans: a plan can contain other sources, but it is meaningless to put a plan
 * inside a chain.
 */
public interface OptionNoState
{

}
