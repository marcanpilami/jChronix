package org.oxymores.chronix.exceptions;

public class ChronixPlanInconsistentException extends ChronixException
{
	private static final long serialVersionUID = 560212617354985100L;

	public ChronixPlanInconsistentException(String message, Exception parent)
	{
		super(message, parent);
	}
}
