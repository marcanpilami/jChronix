package org.oxymores.chronix.exceptions;

public class ChronixNoCalendarException extends ChronixException
{
	private static final long serialVersionUID = -4179968001198693785L;

	public ChronixNoCalendarException()
	{
		super("A state without calendar has no current occurrence");
	}
}
