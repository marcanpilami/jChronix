package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.UUID;

/**
 * An <strong>event source</strong> is a node of the production plan graph, that is an item that can be triggered, then runs and returns a
 * result from which events will be created (the event creation is taken care of by the engine itself, not the source which only returns a
 * "run result" object). Examples are aplenty among the event sources bundled with the default distribution: a shell command, a clock, a
 * file incoming inside a directory...<br>
 * <br>
 * Instances of classes extending EventSource can be either directly created by clients (through a constructor) or deserialised from
 * persistent storage by an {@link EventSourceProvider}. The EventSource has a pointer to the {@link EventSourceProvider} which handles
 * serialisation/deserialisation for itself.<br>
 * <br>
 * Event sources are expected to behave like <strong>Data Transfer Objects (DTO)</strong> - that is, they should be as stable as possible
 * between versions, easy to create, easy to serialise (no complex graph).<br>
 * Note this is not an interface but a base abstract class, so as to allow easier ascending compatibility in the event of an evolution of
 * the core model.
 */
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
     * The {@link EventSourceProvider} that should be used for persistence.
     */
    public abstract Class<? extends EventSourceProvider> getProvider();

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
