package org.oxymores.chronix.api.prm;

import java.util.List;

import org.oxymores.chronix.api.source.EventSourceField;

/**
 * A parameter is a key/value string pair that can be provided to event sources at runtime.<br>
 * The main class of a parameter plugin must implement this interface, which provides both factory and runtime resolution methods.
 */
public interface ParameterProvider
{
    ///////////////////////////////////////////////////////////////////////////
    // Identity Card
    /**
     * The name of the parameter type. Short string with less than 30 characters, like "shell command result".
     */
    public abstract String getName();

    /**
     * A description (fuller than what is given in {@link #getName()}) for when the user asks for details about the plugin.
     */
    public abstract String getDescription();

    /**
     * The description of what the {@link ParameterProvider} created by this behaviour should/may contain.
     */
    public List<EventSourceField> getFields();

    ///////////////////////////////////////////////////////////////////////////
    // Run

    /**
     * The main method of a dynamically resolved parameter provider. <strong>It must be idempotent</strong> as there are no guarantees it
     * will be called only once (also, parameters are resolved even for disabled elements). Said otherwise: dynamic parameters are made to
     * retrieve values, no to modify states.<br>
     * It can either immediately return a value, or trigger an asynchronous resolution process and immediately return null. In the latter
     * case, the result is expected as an {@link AsyncParameterResult} inside a queue which name is given by
     * {@link ParameterResolutionRequest#getReplyToQueueName()}.
     * 
     * @param job
     * @return
     */
    public String getValue(ParameterResolutionRequest job);

}
