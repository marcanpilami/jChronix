package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.UUID;

/**
 * A few methods useful to Event Sources.
 *
 */
public interface EngineCallback
{
    /**
     * Launch asynchronously (the method returns at once) another state. This launch will occur in the scope of the current run (i.e. all
     * the calls to this method will take place in the same dedicated scope).
     */
    public void launchState(DTOState s);

    /**
     * Relays the result of an execution to the engine. Mostly used by asynchronous launches (as synchronous launches can directly return
     * their {@link EventSourceRunResult} as a result of the run method) even if it can be used in all cases (for example to create two
     * results in a single launch).<br>
     * <br>
     * Behind the scenes, this is a composition of {@link #getResultQueueName()} and {@link #sendMessage(Serializable, String)})
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
     * The name of the queue that receives {@link EventSourceRunResult}s for the local engine. Needed when a plugin needs to directly send a
     * result to the engine (when it cannot directly use {@link #sendRunResult(EventSourceRunResult)}, for example because the result is
     * actually created on another JVM)
     */
    public String getResultQueueName();

    /**
     * Sends a JMS message to another Chronix Component. The message is sent in an independent transaction.<br>
     * This is only a helper method to avoid some plugins the hassle of directly interacting with the JMS API.
     * 
     * @param msg
     *            the object that will be serialised inside a JMS ObjectMessage.
     * @param destinationQueue
     *            the name of the destination JMS queue.
     * @param replyTo
     *            name of the reply queue. Can be null.
     */
    public void sendMessage(Serializable msg, String destinationQueue, String replyTo);

    /**
     * The name of the node associated to the current call stack.
     */
    public String getNodeName();
}
