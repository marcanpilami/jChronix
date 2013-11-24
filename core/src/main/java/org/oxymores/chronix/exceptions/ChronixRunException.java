package org.oxymores.chronix.exceptions;

public class ChronixRunException extends ChronixException
{
    private static final long serialVersionUID = -1855297427956762130L;

    public ChronixRunException(String message)
    {
        super(message);
    }

    public ChronixRunException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
