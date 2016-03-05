package org.oxymores.chronix.api.agent;

/**
 * The exception to throw by a message listener so as to signal that the messages should be rolled-back. (default is: messages read or
 * written are always committed - even if an exception is thrown, since most of the time a failing message will fail again when re-read).
 */
public class ListenerRollbackException extends RuntimeException
{
    private static final long serialVersionUID = 8148157585278132533L;

    public ListenerRollbackException(String message, Exception e)
    {
        super(message, e);
    }
}
