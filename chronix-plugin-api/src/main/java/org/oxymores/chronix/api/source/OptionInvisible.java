package org.oxymores.chronix.api.source;

/**
 * By implementing this interface (which is empty) an {@link EventSource} signals the engine that its results should not appear inside the
 * main history table. This is useful for highly technical event sources: for example, an AND door will never create any interesting log for
 * the end user.
 */
public interface OptionInvisible
{

}
