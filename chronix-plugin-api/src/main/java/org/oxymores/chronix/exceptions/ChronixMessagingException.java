package org.oxymores.chronix.exceptions;

public class ChronixMessagingException extends ChronixException
{
    private static final long serialVersionUID = 1436037468232008459L;

    public ChronixMessagingException(String message)
    {
        super(message);
    }

    public ChronixMessagingException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
