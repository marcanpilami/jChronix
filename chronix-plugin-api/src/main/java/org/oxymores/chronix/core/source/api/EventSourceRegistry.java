package org.oxymores.chronix.core.source.api;

import java.util.UUID;

/**
 * This is an interface to the registry - that is, the engine module that stores all active event source instances.
 */
public interface EventSourceRegistry
{
    /**
     * Retrieves an event source instance from the loaded instances cache. If none found, null is returned.<br>
     * {@link EventSourceProvider} instances are expected to use this method to fetch the instances of their own DTOs.<br>
     * Only registered instances are returned.
     * 
     * @param id
     *            the unique ID of the event source instance.
     * @return
     */
    public EventSource getEventSource(UUID id);

    /**
     * Allow the engine to use an event source. Used during the startup sequence. Should usually be called in the
     * {@link EventSourceProvider#deserialise(java.io.File, EventSourceRegistry)} method.
     * 
     * @param source
     */
    public <T extends EventSource> void registerSource(T source);

    /**
     * The opposite of {@link #registerSource(EventSource)}. It makes the source unavailable to new jobs (running jobs are not affected).
     * 
     * @param source
     */
    public <T extends EventSource> void unregisterSource(T source);
}
