package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.UUID;

public abstract class EventSource implements Serializable
{
    private static final long serialVersionUID = -1239160226415660389L;

    protected UUID id = UUID.randomUUID();

    /**
     * Every event source instance has an unique ID. We use statistically unique UUID since this is an easy solution for uniqueness in
     * distributed systems like Chronix.
     */
    public UUID getId()
    {
        return this.id;
    }

    /**
     * A name, less than 20 characters.
     */
    public abstract String getName();

    /**
     * Event sources can be disabled by returning false. In this case, every State that uses this sources will run in disabled mode (i.e.
     * the {@link EventSourceBehaviour#runDisabled(EngineCallback, JobDescription)} will be called instead of calling
     * {@link EventSourceBehaviour#run(EngineCallback, JobDescription)}.
     */
    public boolean isEnabled()
    {
        return true;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Run methods

    /**
     * The main method of an event source plugin. It is called by the engine when it has determined the event source should actually run.
     * This method can do pretty much anything it needs: run Java code locally, call the runner agent (and one of its own plugins), ... <br>
     * <br>
     * This method is expected to run <strong>synchronously</strong> by default. In that case, it is expected to return quickly (less than a
     * second on most platforms).<br>
     * To run <strong>asynchronously</strong>, this method can create its own thread (or call an external system, or any asynchronous system
     * available...) and then return a null RunResult. In that case, the true RunResult is expected to arrive later on the RUNNER queue.
     * 
     * @return
     */
    public abstract EventSourceRunResult run(EngineCallback cb, JobDescription jd);

    /**
     * This method simulates the result that a job would have had if it has ended OK, without actually running the job. (this is needed
     * because the interpretation of "OK" depends of the plugin)
     */
    public EventSourceRunResult runForceOk(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "Source forced OK";

        return rr;
    }

    public EventSourceRunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult rr = new EventSourceRunResult();
        rr.returnCode = 0;
        rr.logStart = "Source disabled - doing as if it had ended OK";

        return rr;
    }

    // Run methods
    ///////////////////////////////////////////////////////////////////////////

}
