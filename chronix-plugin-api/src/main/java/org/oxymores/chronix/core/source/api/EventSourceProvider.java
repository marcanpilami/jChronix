package org.oxymores.chronix.core.source.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The base class to extend for all event source plugins. It contains some metadata and allows to store and fetch event source instances.
 * <br>
 * All event source providers (classes extending this one) should be exposed as an OSGI service implementing contract EventSourceProvider.
 * <br>
 * <br>
 * Note this is not an interface but a base abstract class, so as to allow easier ascending compatibility in the event of an evolution of
 * the core model.
 */
public abstract class EventSourceProvider
{
    ///////////////////////////////////////////////////////////////////////////
    // Identity Card
    /**
     * The name of this event source type, as it should appear in the log and the web pages.<br>
     * If the result is expected to be stable with time, it is not used anywhere but in user interfaces so a change is not catastrophic.
     */
    public abstract String getSourceName();

    // TODO: a localised variant.

    /**
     * The description of this event source type, as it should appear in the log and the web pages. See {@link #getSourceName()} for
     * stability.
     */
    public abstract String getSourceDescription();
    // Identity Card
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Serialisation
    /**
     * This method is called when the production plan is being serialised to disk. The implementation is required to write one and only one
     * file at the designated path. If the file already exists, it must be overwritten.<br>
     * The implementation is free to use any serialisation method. The use of XStream is however recommended as this bundle is always
     * present for the engine needs.<br>
     * <br>
     * Also, see DDDDDDDDD interface to ease serialisation.
     * 
     * @param targetFile
     */
    public void serialise(File targetFile, Collection<? extends EventSource> instances)
    {

    }

    /**
     * The reverse method of {@link #serialise(File)}. <br>
     * <br>
     * <strong>This method is supposed to cope with model version upgrades</strong>. That is, if the given <code>File</code> contains
     * serialised objects related to a previous version of the model, this method will either successfully convert them to the latest
     * version or throw a runtime exception.<br>
     * <br>
     * If any, the deserialised sources should be converted to an object implementing {@link EventSource} and registered through the
     * {@link EngineCallback}
     * 
     * @param sourceFile
     *            a directory containing the serialised data.
     * @param reg
     *            for registering the deserialised items inside the engine.
     * @return
     */
    public abstract void deserialise(File sourceFile, EventSourceRegistry reg);
    // Serialisation
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Construction

    /**
     * As the plugins can declare any DTO they like, this can cause issues to frameworks that rely on class discovery inside some class
     * loaders. This method actually returns all the Class objects that can be used outside the plugin so to allow to explicitly register
     * them inside these frameworks.<br>
     * Default is an empty list. <br>
     * Marked for deprecation - we hope we won't need this in this end.
     * 
     * @return
     */
    @Deprecated
    public List<Class<? extends EventSource>> getExposedDtoClasses()
    {
        return new ArrayList<>();
    }

    // Construction
    ///////////////////////////////////////////////////////////////////////////

}
