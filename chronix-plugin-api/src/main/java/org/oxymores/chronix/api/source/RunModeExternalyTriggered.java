package org.oxymores.chronix.api.source;

/**
 * An event source that comes from an external system (this is the only type of event source for which the trigger is not done by the
 * engine)
 */
public interface RunModeExternalyTriggered
{
    /**
     * The main method of a triggered event source plugin. It is called by the engine when it has determined the event source should
     * actually run. This method can do pretty much anything it needs: run Java code locally, call the runner agent (and one of its own
     * plugins), ... <br>
     * <br>
     * This method is expected to run <strong>synchronously</strong> by default. In that case, it is expected to return quickly (less than a
     * second on most platforms).<br>
     * To run <strong>asynchronously</strong>, this method can create its own thread (or call an external system, or any asynchronous system
     * available...) and then return a null RunResult. In that case, the true RunResult must be sent later by using the provided
     * EngineCallback.
     */
    public abstract EventSourceRunResult run(DTOEventSource source, EngineCallback cb, JobDescription jd);
}
