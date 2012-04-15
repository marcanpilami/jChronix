package org.oxymores.chronix.core;

import java.util.ArrayList;

public class Parameter extends MetaObject {

	private static final long serialVersionUID = 8017529181151172909L;

	protected String key, value, description;
	protected Boolean reusable = false;
	
	protected Application application;
	protected ArrayList<ConfigNodeBase> elements;
	
	public Parameter()
	{
		super();
		elements = new ArrayList<ConfigNodeBase>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getReusable() {
		return reusable;
	}

	public void setReusable(Boolean reusable) {
		this.reusable = reusable;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public ArrayList<ConfigNodeBase> getElements() {
		return elements;
	}
	
	public void addElement(ConfigNodeBase element)
	{
		if (!elements.contains(element))
		{
			elements.add(element);
			element.addParameter(this);
		}
	}
}
