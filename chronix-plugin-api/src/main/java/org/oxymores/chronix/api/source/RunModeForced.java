package org.oxymores.chronix.api.source;

/**
 * Implementing this interface tells the engine that an event source has a specific way of running a "force OK" order. If not implemented,
 * the engine simply simulates a run that result in a 0 result code.
 */
public interface RunModeForced
{
    /**    
     */
    public abstract EventSourceRunResult runForceOk(DTOEventSource source, EngineCallback cb, JobDescription jd);
}
