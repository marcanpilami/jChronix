package org.oxymores.chronix.core;

public class ActiveNodeBase extends ConfigNodeBase {
	private static final long serialVersionUID = 2317281646089939267L;
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	protected String description;
	protected String name;
}
