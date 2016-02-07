package org.oxymores.chronix.core.source.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The base class to extend for all event source plugins. <br>
 * An <strong>event source</strong> is a node of the production plan graph, that is an item that can be triggered (the source itself having
 * the final word on whether the events coming from other sources should trigger this one or not), then runs and returns a result from which
 * events will be created (the event creation is taken care of by the engine itself, not the source which only returns a "run result"
 * object).<br>
 * <br>
 * Note this is not an interface but a base abstract class, so as to allow easier ascending compatibility in the event of an evolution of
 * the core model. BEHAVIOUR ???
 */
public abstract class EventSourceBehaviour
{
    ///////////////////////////////////////////////////////////////////////////
    // Identity Card
    /**
     * The name of this event source type, as it should appear in the log and the web pages.
     */
    public abstract String getSourceName();

    // TODO: a localized variant.

    /**
     * The description of this event source type, as it should appear in the log and the web pages.
     */
    public abstract String getSourceDescription();
    // Identity Card
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Serialization
    /**
     * This method is called when the production plan is being serialized to disk. The implementation is required to write one and only one
     * file at the designated path. If the file already exists, it must be overwritten.<br>
     * The implementation is free to use any serialization method. The use of XStream is however recommended as this bundle is always
     * present for the engine needs.<br>
     * <br>
     * Also, see DDDDDDDDD interface to ease serialization.
     * 
     * @param targetFile
     */
    public void serialize(File targetFile, Collection<? extends DTO> instances)
    {

    }

    /**
     * The reverse method of {@link #serialize(File)}. <br>
     * <br>
     * <strong>This method is supposed to cope with model version upgrades</strong>. That is, if the given <code>File</code> contains
     * serialized objects related to a previous version of the model, this method will either successfully convert them to the latest
     * version or throw an FFFFException.<br>
     * <br>
     * It any, the deserialized sources should be converted to an object implementing {@link DTO} and registered through the
     * {@link EngineCallback}
     * 
     * @param sourceFile
     * @return
     */
    public abstract void deserialize(File sourceFile, EngineCallback cb);
    // Serialization
    ///////////////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////////////
    // Construction

    // public Object createSource();

    // public DTO internalToDto();

    // newDto

    // dto2Internal

    /**
     * As the plugins can declare any DTO they like, this can cause issues to frameworks that rely on class discovery inside some
     * classloaders. This method actually returns all the Class objects that can be used outside the plugin so to allow to explicitly
     * register them inside these frameworks.<br>
     * Default is an empty list.
     * 
     * @return
     */
    public List<Class<? extends DTO>> getExposedDtoClasses()
    {
        return new ArrayList<>();
    }

    // Construction
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Toggles

    /**
     * Should the node execution results be visible in the history table, or should it be hidden (low end-user value)?
     */
    public boolean visibleInHistory()
    {
        return true;
    }

    /**
     * Should it be executed by the self-trigger agent?
     */
    public boolean selfTriggered()
    {
        return false;
    }

    //
    ///////////////////////////////////////////////////////////////////////////
}
