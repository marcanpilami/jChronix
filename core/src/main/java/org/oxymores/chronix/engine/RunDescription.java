package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.ArrayList;

public class RunDescription implements Serializable {

	private static final long serialVersionUID = -7603747840000703435L;

	public String reportToQueueName;
	public String command;
	public String parameters;
	public ArrayList<String> envNames;
	public ArrayList<String> envValues;
	
	public String Method = "Shell";
}
