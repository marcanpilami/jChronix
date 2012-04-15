package org.oxymores.chronix.exceptions;

public class ChronixException extends Exception {

	private static final long serialVersionUID = 6684815812625785453L;

	public ChronixException()
	{
		super();
	}
	
	public ChronixException(String message)
	{
		super(message);
	}
}
