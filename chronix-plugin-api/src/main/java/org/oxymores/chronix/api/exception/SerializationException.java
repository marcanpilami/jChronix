package org.oxymores.chronix.api.exception;

public class SerializationException extends ChronixPluginException
{
    private static final long serialVersionUID = -969284128372941010L;

    public SerializationException()
    {
        super();
    }

    public SerializationException(String message)
    {
        super(message);
    }

    public SerializationException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
