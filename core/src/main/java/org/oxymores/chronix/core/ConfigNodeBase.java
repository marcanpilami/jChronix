package org.oxymores.chronix.core;

import java.util.ArrayList;

public class ConfigNodeBase extends MetaObject {
	private static final long serialVersionUID = 4288408733877784921L;
	
	protected Application Application;
	protected ArrayList<Parameter> parameters;
	
	public ConfigNodeBase()
	{
		super();
		parameters = new ArrayList<Parameter>();
	}
	
	
	public Application getApplication() {
		return Application;
	}
	public void setApplication(Application application) {
		Application = application;
	}
	
	public ArrayList<Parameter> getParameters()
	{
		return this.parameters;
	}
	
	public void addParameter(Parameter parameter)
	{
		if (! parameters.contains(parameter))
		{
			parameters.add(parameter);
			parameter.addElement(this);
		}
	}
}
