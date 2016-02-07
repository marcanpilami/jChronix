package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.UUID;

import org.oxymores.chronix.engine.modularity.runner.RunResult;

/**
 * A few methods useful to Event Sources.
 *
 */
public interface EngineCallback
{
    /**
     * Sends a JMS message to another Chronix Component.
     * 
     * @param msg
     *            the object that will be serialised inside a JMS ObjectMessage.
     * @param destinationQueue
     *            the name of the destination JMS queue.
     */
    public void sendMessageXCXXXXX(Object msg, String destinationQueue);

    /**
     * Relays the result of an execution to the engine. Mostly used by asynchronous launches (as synchronous launches can directly return
     * their {@link EventSourceRunResult} as a result of the run method) even if it can be used in all cases (for example to create two
     * results in a single launch).
     */
    public void sendRunResult(EventSourceRunResult r);

    /**
     * Retrieves an event source instance from the loaded instances cache. If none found, null is returned.<br>
     * {@link EventSourceBehaviour} instances are expected to use this method to fetch the instances of their own DTOs.
     * 
     * @param id
     * @return
     */
    public DTO getEventSource(UUID id);

    /**
     * Allow the engine to use an event source. Used during the startup sequence. Should usually be called in the
     * {@link EventSourceBehaviour#deserialize(java.io.File, EngineCallback)} method.
     * 
     * @param source
     */
    public <T extends DTO & Serializable> void registerSource(T source);

    /**
     * The opposite of {@link #registerSource(DTO)}. It makes the source unavailable to new jobs (running jobs are not affected).
     * 
     * @param source
     */
    public <T extends DTO> void unregisterSource(T source);
}
