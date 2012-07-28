package org.oxymores.chronix.core.timedata;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang.time.DateFormatUtils;

@Entity
public class RunLog implements Serializable {
	private static final long serialVersionUID = 154654512882124L;

	@Column(length = 20)
	public String lastKnownStatus;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	@Id
	public String id; // UUID (is the id of the pipelinejob)
	public String chainName;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String chainId; // UUID
	public String chainLev1Name;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String chainLev1Id; // UUID
	public String applicationName;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String applicationId; // UUID
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String stateId; // UUID
	public String activeNodeName;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String activeNodeId; // UUID
	public String placeName;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String placeId; // UUID
	public String executionNodeName;
	@Column(columnDefinition = "CHAR(36)", length = 36)
	public String executionNodeId; // UUID
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
	public String calendarName;
	public String calendarOccurrence;
	public String logPath;

	public static String getTitle() {
		String res = "";
		res = String
				.format("%-36s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %s | %s | %s | %s | %-15s | %-15s | %-10s",
						"ID", "chainName", "applicationName", "activeNodeName",
						"osAccount", "whatWasRun", "RC", "enteredPipeAt ",
						"beganRunningAt", "stoppedRunning", "markedForUnAt ",
						"calendarName", "calendar occr", "logPath");
		return res;
	}

	public String getLine() {
		String res = "";
		res = String
				.format("%36s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %s | %s | %s | %s | %-15s | %-15s | %-10s",
						this.id,
						chainName.substring(0, Math.min(19, chainName.length())),
						applicationName.substring(0,
								Math.min(19, applicationName.length())),
						activeNodeName.substring(0,
								Math.min(19, activeNodeName.length())),
						osAccount,
						whatWasRun,
						resultCode,
						enteredPipeAt == null ? null : DateFormatUtils.format(
								enteredPipeAt, "dd/MM HH:mm:ss"),
						beganRunningAt == null ? null : DateFormatUtils.format(
								beganRunningAt, "dd/MM HH:mm:ss"),
						stoppedRunningAt == null ? null : DateFormatUtils
								.format(stoppedRunningAt, "dd/MM HH:mm:ss"),
						markedForUnAt == null ? null : DateFormatUtils.format(
								markedForUnAt, "dd/MM HH:mm:ss"),
						calendarName == null ? null : calendarName.substring(0,
								Math.min(14, calendarName.length())),
						calendarOccurrence == null ? null : calendarOccurrence
								.substring(
										0,
										Math.min(19,
												calendarOccurrence.length())),
						logPath == null ? null : logPath.substring(0,
								Math.min(9, logPath.length())));
		return res;
	}
}
