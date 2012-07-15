package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public class RunResult implements Serializable {

	private static final long serialVersionUID = 316559310140465996L;

	public String logStart = "";
	public String logPath = "";
	public int returnCode = -1;
	public HashMap<String, String> newEnvVars = new HashMap<String, String>();
	
	
	// Data below is from the engine - not created by the run
	public String id1 = null;
	public UUID id2 = null;
	public Boolean outOfPlan = false;
}
