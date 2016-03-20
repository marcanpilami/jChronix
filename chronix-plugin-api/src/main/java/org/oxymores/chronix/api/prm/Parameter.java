package org.oxymores.chronix.api.prm;

import java.util.UUID;

/**
 * A <strong>Parameter</strong> is an object that can return a {@link String} value which can be fed to Event Sources (the actual elements
 * doing some work). <br>
 */
public abstract class Parameter
{
    UUID id = UUID.randomUUID();

    /**
     * The main method of a {@link Parameter}. It returns the value that should be used as an event source parameter.<br>
     * This method can either return synchronously or asynchronously.<br>
     * <ol>
     * <li><strong>Synchronously</strong>: the value to use is returned directly by this method</li>
     * <li><strong>Asynchronously</strong>: this method returns null, and an {@link AsyncParameterResult} must be sent to the queue given as
     * a parameter with a JMS header called "prmLaunchId" containing the value given as a parameter</li>
     * </ol>
     * Whatever the method used, the end result is never null but can be an empty string ("").
     */
    public abstract String getValue(String replyQueueName, String prmLaunchId);

    /**
     * Every Parameter instance has an unique ID that allows the engine to retrieve them from plugins. Use of statistically unique UUID is
     * recommended since this is an easy solution for uniqueness in distributed systems like Chronix.
     */
    public UUID getId()
    {
        return this.id;
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof Parameter))
        {
            return false;
        }
        return ((Parameter) obj).getId().equals(this.id);
    }

    /**
     * A parameter relies on a specific {@link ParameterProvider} type to be persisted. This allows the engine to find the correct provider
     * when it is simply given the Parameter object and nothing more.
     */
    public abstract Class<? extends ParameterProvider> getProvider();
}
