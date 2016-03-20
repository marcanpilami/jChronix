package org.oxymores.chronix.api.exception;

/**
 * The base class for all exceptions defined in the different Chronix plugin APIs.
 */
public class ChronixPluginException extends RuntimeException
{
    private static final long serialVersionUID = -6161533173109648696L;

    public ChronixPluginException()
    {
        super();
    }

    public ChronixPluginException(String message)
    {
        super(message);
    }

    public ChronixPluginException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
