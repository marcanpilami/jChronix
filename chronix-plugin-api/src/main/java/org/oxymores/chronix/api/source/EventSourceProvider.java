package org.oxymores.chronix.api.source;

import java.util.List;

import org.oxymores.chronix.api.exception.ModelUpdateException;
import org.oxymores.chronix.core.engine.api.DTOApplication;

/**
 * The main class of an event source plugin. It defines both a factory to create event source instances and the behaviour of said event
 * sources.<br>
 * It is intended to be completed with different options, each defined with another interface.
 */
public interface EventSourceProvider
{
    ///////////////////////////////////////////////////////////////////////////
    // Self description
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The name of the source, as will be displayed in the different UIs. Only used for display purposes.
     * 
     * @return the name
     */
    public String getName();

    /**
     * A short (< 255 characters) description for "details" panels in the UIs. Describes what the event source does.
     * 
     * @return the description
     */
    public String getDescription();

    /**
     * The description of what the {@link DTOEventSource} created by this behaviour should/may contain.
     */
    public List<EventSourceField> getFields();

    ///////////////////////////////////////////////////////////////////////////
    // Meta manipulation
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This method is called whenever a new application is created. This allows a plugin to add items to the application and is mostly
     * useful for singleton source plugins.<br>
     * Default implementation is empty.
     * 
     * @param newApp
     *            the application being created. Should be directly modified by this method.
     */
    public default void onNewApplication(DTOApplication newApp)
    {
        return;
    }

    /**
     * This method is called whenever an application is opened by the engine or an API. This allows to add and remove sources - this is
     * mostly useful for singleton plugins which have been added after an application has been created - since they were not present at
     * creation time, their {@link #onNewApplication(DTOApplication)} method was not called and they could not add their data to the
     * application. This method gives them another chance to do it.<br>
     * Default implementation simply calls {@link #onNewApplication(DTOApplication)} (which by default does nothing). So if this latter
     * method is implemented in an idempotent way, there is no need to implement this. <br>
     * <br>
     * Beware: this method is a place to add and remove event sources, not a place to modify sources. Source metadata update is handled in
     * another method.
     * 
     * 
     * @param app
     *            the application being hydrated. Should be directly modified by this method.
     */
    public default void onDeserialisedApplication(DTOApplication app)
    {
        onNewApplication(app);
    }

    /**
     * This method is called by the engine when it loads a source which was created by a previous version of its plugin. This method should
     * if needed update the event source object so that it stays compatible with the plugin. A {@link ModelUpdateException} should be raised
     * if this is not possible.<br>
     * Default implementation does nothing. This method should be implemented the first time a plugin changes its fields.
     * 
     * @param source
     * @param version
     *            the version of the metadata. The version of the package containing the service.
     */
    public default void upgradeSource(DTOEventSource source, String version)
    {
        return;
    }

    public default void onNewSource(DTOEventSource source, DTOApplication app)
    {
        return;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The engine calls this method to get the correct interpretation of an event and a transition definition. This is the one method that
     * "gives meaning" to event data (the very same data that was created by the source).<br>
     * It is given a transition originating from this source, the event (created by this source) to analyse, and the place on which it
     * should be analysed.<br>
     * The method must focus on giving meaning to the event and transition data - it should not take anything else into account (such as
     * calendars, tokens...)
     */
    public boolean isTransitionPossible(DTOEventSource source, DTOTransition tr, DTOEvent event);
}
