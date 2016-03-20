package org.oxymores.chronix.api.exception;

public class ModelUpdateException extends ChronixPluginException
{
    private static final long serialVersionUID = -969284128372941010L;

    public ModelUpdateException()
    {
        super();
    }

    public ModelUpdateException(String message)
    {
        super(message);
    }

    public ModelUpdateException(String message, Exception innerException)
    {
        super(message, innerException);
    }
}
