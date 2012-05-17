package org.oxymores.chronix.core.timedata;

import java.util.Date;
import java.util.UUID;

public class RunLog {
	public String lastKnownStatus;
	public UUID id;
	public UUID pipeJobId;
	public String chainName;
	public UUID chainId;
	public String chainLev1Name;
	public UUID chainLev1Id;
	public String applicationName;
	public UUID applicationId;
	public UUID stateId;
	public String activeNodeName;
	public UUID activeNodeId;
	public String placeName;
	public UUID placeId;
	public String executionNodeName;
	public UUID executionNodeId;
	public String dns;
	public String osAccount;
	public String whatWasRun;
	public String shortLog;
	public int resultCode;
	public Date enteredPipeAt;
	public Date markedForUnAt;
	public Date beganRunningAt;
	public Date stoppedRunningAt;
	public long dataIn, dataOut;
	public long sequence;
	public Date calendarDay;
	public boolean sent;
}
