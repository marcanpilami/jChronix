package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class RunResult implements Serializable {

	private static final long serialVersionUID = 316559310140465996L;

	public String logStart = "";
	public String fullerLog = "";
	public String logPath = "";
	public int returnCode = -1;
	public String conditionData2 = null;
	public String conditionData3 = null;
	public String conditionData4 = null; // Actually UUID
	public HashMap<String, String> newEnvVars = new HashMap<String, String>();
	public Date start, end;

	// Data below is from and for the engine - not created by the run
	public String id1 = null;
	public UUID id2 = null;
	public Boolean outOfPlan = false;
}
