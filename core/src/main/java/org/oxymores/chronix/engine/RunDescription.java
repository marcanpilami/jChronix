package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class RunDescription implements Serializable {

	private static final long serialVersionUID = -7603747840000703435L;

	public String reportToQueueName;
	public String command;
	//public String parameters;
	public ArrayList<String> paramNames = new ArrayList<String>();
	public ArrayList<String> paramValues = new ArrayList<String>();
	public ArrayList<String> envNames = new ArrayList<String>();
	public ArrayList<String> envValues = new ArrayList<String>();
	
	public Boolean helperExecRequest = false;
	
	public String Method = "Shell";
	
	
	// This data is only useful for the engine, not the runner.
	// It should be put as is in the run result object.
	public String id1;
	public UUID id2;
	public Boolean outOfPlan = false;
}
