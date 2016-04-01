package org.oxymores.chronix.api.source;

/**
 * The most common type of {@link EventSource}.<br>
 * A <strong>triggered</strong> event source can be run by the engine. In other words, it can both "receive arrows" and "send arrows".
 *
 */
public abstract class EventSourceTriggered extends EventSource
{
    private static final long serialVersionUID = 9109316248883893408L;

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
    public abstract EventSourceRunResult run(EngineCallback cb, JobDescription jd);

    /**
     * This method simulates the result that a job would have had if it has ended OK, without actually running the job. (this is needed
     * because the interpretation of "OK" depends of the plugin).<br>
     * Default implementation simply returns a RunResult with a "0" return code.
     */
    public EventSourceRunResult runForceOk(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "Source forced OK";

        return rr;
    }

    /**
     * This method is called for running disabled states. It is needed because some event sources will actually need to run things event in
     * a disabled state. For example, a disabled chain may want to run all its content disabled.<br>
     * Default implementation simply returns a RunResult with a "0" return code and shold be enough for most cases.
     */
    public EventSourceRunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "Source disabled - doing as if it had ended OK";

        return rr;
    }
}
