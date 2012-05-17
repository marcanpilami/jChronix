package org.oxymores.chronix.core;

public class EnvironmentParameter extends ApplicationObject {

	private static final long serialVersionUID = -3573665084125546426L;

	String key, value;
	
	public EnvironmentParameter(String key, String value)
	{
		super();
		this.key = key;
		this.value = value;
	}
}
