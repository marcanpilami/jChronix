package org.oxymores.chronix.api.source;

/**
 * By implementing this interface (which is empty) an {@link EventSourceTriggered} signals the engine that States implementing this event
 * source should never be the source of a transition. (by default, {@link EventSourceTriggered} can both be the source and the target of
 * transitions).
 */
public interface EventSourceOptionCannotEmit
{

}
