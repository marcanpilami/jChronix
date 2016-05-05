package org.oxymores.chronix.api.source;

import java.util.List;

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
    // Factory
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The only way to create event source instances. The created object is added to the application passed in parameter.
     * 
     * @param name
     *            the name to give to the instance
     * @param description
     *            a short description (255 characters max)
     * @param app
     *            the application that the new source is added to
     * @param parameters
     *            an array of elements expected by the provider to create an instance. This depends on the provider. Usually will be an
     *            array of {@link DTOParameter}s.
     * @return
     */
    public DTOEventSource newInstance(String name, String description, DTOApplication app, Object... parameters);

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
