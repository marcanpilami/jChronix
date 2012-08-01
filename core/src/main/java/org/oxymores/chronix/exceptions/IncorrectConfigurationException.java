package org.oxymores.chronix.exceptions;

public class IncorrectConfigurationException extends ChronixException {

	private static final long serialVersionUID = 1250426298228613020L;

	public IncorrectConfigurationException(String erroneousParameter) {
		super(String.format("The configuration is incorrect : %s",
				erroneousParameter));
	}
}
