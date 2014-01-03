package org.oxymores.chronix.exceptions;

public class ChronixInitializationException extends ChronixException
{
    private static final long serialVersionUID = 5529273767567084081L;

    public ChronixInitializationException(String message)
    {
        super(message);
    }

    public ChronixInitializationException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
