package org.oxymores.chronix.api.source;

/**
 * An event source that comes from an external system. The actual trigger is done through an engine API (so from the plugin perspective, it
 * may seem that the trigger is done by the engine, even if the engine in this case is only a router) <br>
 * This is the only type of event source for which the trigger is not done by the engine. <br>
 * This is useful for coupling the plan with external system and middlewares (end of file transfer, ...)<br>
 * <br>
 * It is very important to note that events created by such a source are always on the global scope - therefore, they can only be used in
 * top level containers. (It would be impossible to reliably link an external event to a specific scope: how could we know that the arrival
 * of file invoice.csv is should be inside launch 654 of a container?)
 */
public interface RunModeExternallyTriggered
{
    /**
     * The main method of an externally triggered event source plugin. <br>
     * <br>
     * This type of event source creates a single event on each run. The event itself is created by the caller with the content of the
     * returned {@link EventSourceRunResult} object. It is expected to be very simple and simply create events as requested. This method is
     * expected to always run <strong>synchronously</strong> and take at most a few seconds on most platforms<br>
     */
    public abstract EventSourceRunResult run(DTOEventSource source, JobDescription jd);
}
