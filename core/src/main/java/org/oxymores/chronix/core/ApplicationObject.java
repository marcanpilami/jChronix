package org.oxymores.chronix.core;

public class ApplicationObject extends ChronixObject {

	private static final long serialVersionUID = -926121748083888054L;

	protected Application application;

	public Application getApplication() {
		return application;
	}

	// No access modifier: package private. Should only be called by Application
	// (inside an addObject method)
	void setApplication(Application application) {
		this.application = application;
	}
}
