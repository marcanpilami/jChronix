package org.oxymores.chronix.core.source.api;

import java.util.UUID;

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
     * {@link EventSourceProvider} instances are expected to use this method to fetch the instances of their own DTOs.
     * 
     * @param id
     * @return
     */
    public EventSource getEventSource(UUID id);

    /**
     * Launch asynchronously (the method returns at once) another state. This launch will occur in the scope of the current run (i.e. all
     * the calls to this method will take place in the same dedicated scope).
     */
    public void launchState(DTOState s);
}
