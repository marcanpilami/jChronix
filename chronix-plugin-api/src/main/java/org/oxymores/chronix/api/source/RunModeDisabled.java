package org.oxymores.chronix.api.source;

/**
 * A {@link RunModeTriggered} may implement this to control what will be run when the engine runs the source as "disabled". If not
 * implemented, the engine will actually create a run result with a return code of 0.
 */
public interface RunModeDisabled
{
    public abstract EventSourceRunResult runDisabled(DTOEventSource source, EngineCallback cb, JobDescription jd);
}
