package org.oxymores.chronix.exceptions;

public class ChronixException extends Exception {

	private static final long serialVersionUID = 6684815812625785453L;
	
	private Exception innerException;
	
	public ChronixException()
	{
		super();
	}
	
	public ChronixException(String message)
	{
		super(message);
	}
	
	public ChronixException(String message, Exception innerException)
	{
		super(message, innerException);
		this.innerException = innerException;
	}
	
	@Override
	public String toString() {
		if (this.innerException != null)
			return this.getMessage() + "\n" + this.innerException.getMessage() + "\n\n" + this.getStackTrace().toString();
		else
			return this.getMessage() + "\n\n" + this.getStackTrace().toString();
	}
}
