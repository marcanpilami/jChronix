/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership. This file is licensed to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package org.oxymores.chronix.core.timedata;

import java.io.Serializable;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.sql2o.Connection;

public class RunLog implements Serializable
{
    private static final long serialVersionUID = 154654512882124L;
    private static final int DESCR_LENGTH = 100;
    private static final int PATH_LENGTH = 1024;
    private static final int LOG_LENGTH = 10000;
    private static final String DATE_FORMAT = "dd/MM HH:mm:ss";
    private static final DateTimeFormatter JODA_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);

    // /////////////////////////////
    // Main ID is an UUID which is also the id of the pipelinejob)
    @NotNull
    private UUID id;

    private Boolean visible = true;

    // /////////////////////////////
    // Plan elements definition
    private UUID applicationId;

    private UUID chainId;

    private UUID stateId;

    private UUID activeNodeId;

    private UUID chainLev1Id;

    private UUID executionNodeId;

    private UUID placeId;

    private UUID chainLaunchId;

    // /////////////////////////////
    // Plan element names (helpers for end user + enables to be totally independent from plan definition)
    @Size(min = 1, max = DESCR_LENGTH)
    private String chainName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String applicationName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String chainLev1Name;

    @Size(min = 1, max = DESCR_LENGTH)
    private String activeNodeName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String placeName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String executionNodeName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String dns;

    @Size(min = 1, max = DESCR_LENGTH)
    private String osAccount;

    // ///////////////////////////////
    // Status / result
    @Size(min = 1, max = PATH_LENGTH)
    private String whatWasRun;

    private Integer resultCode;

    @Size(min = 1, max = 20)
    private String lastKnownStatus;

    @Size(min = 1, max = LOG_LENGTH)
    private String shortLog;

    @Size(min = 1, max = PATH_LENGTH)
    private String logPath;

    // /////////////////////////////
    // Data/ capa planning
    private long dataIn, dataOut;

    // /////////////////////////////
    // Calendar & seq
    @Size(min = 1, max = DESCR_LENGTH)
    private String calendarName;

    @Size(min = 1, max = DESCR_LENGTH)
    private String calendarOccurrence;

    private long sequence;

    // /////////////////////////////
    // Dates
    private DateTime enteredPipeAt;

    private DateTime markedForUnAt;

    private DateTime beganRunningAt;

    private DateTime stoppedRunningAt;

    private DateTime lastLocallyModified;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper accessors
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    public static String getTitle()
    {
        String res = String.format(
                "%-36s | %-20s | %-20s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %-14s | %-14s | %-14s | %-14s | %-15s | %-15s | %-10s | %-5s | %-36s",
                "ID", "placename", "execnodename", "chainName", "applicationName", "activeNodeName", "osAccount", "whatWasRun", "RC",
                "enteredPipeAt ", "beganRunningAt", "stoppedRunning", "markedForUnAt ", "calendarName", "calendar occr", "logPath", "visib",
                "chainLaunchId");
        return res;
    }

    public String getLine()
    {

        String res = String.format(
                "%36s | %-20s | %-20s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %-14s | %-14s | %-14s | %-14s | %-15s | %-15s | %-10s | %-5s | %36s",
                this.id, this.placeName.substring(0, Math.min(19, placeName.length())), this.executionNodeName,
                chainName.substring(0, Math.min(19, chainName.length())),
                applicationName.substring(0, Math.min(19, applicationName.length())),
                activeNodeName.substring(0, Math.min(19, activeNodeName.length())),
                osAccount == null ? "" : osAccount.substring(0, Math.min(10, osAccount.length())),
                whatWasRun == null ? "" : whatWasRun.substring(0, Math.min(29, whatWasRun.length())), resultCode,
                enteredPipeAt == null ? null : enteredPipeAt.toString(JODA_FORMATTER),
                beganRunningAt == null ? null : beganRunningAt.toString(JODA_FORMATTER),
                stoppedRunningAt == null ? null : stoppedRunningAt.toString(JODA_FORMATTER),
                markedForUnAt == null ? null : markedForUnAt.toString(JODA_FORMATTER),
                calendarName == null ? null : calendarName.substring(0, Math.min(14, calendarName.length())),
                calendarOccurrence == null ? null : calendarOccurrence.substring(0, Math.min(19, calendarOccurrence.length())),
                logPath == null ? null : logPath.substring(0, Math.min(9, logPath.length())), visible, chainLaunchId);
        return res;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getLastKnownStatus()
    {
        return lastKnownStatus;
    }

    public void setLastKnownStatus(String lastKnownStatus)
    {
        this.lastKnownStatus = lastKnownStatus;
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public UUID getChainId()
    {
        return chainId;
    }

    public void setChainId(UUID chainId)
    {
        this.chainId = chainId;
    }

    public String getChainLev1Name()
    {
        return chainLev1Name;
    }

    public void setChainLev1Name(String chainLev1Name)
    {
        this.chainLev1Name = chainLev1Name;
    }

    public UUID getChainLev1Id()
    {
        return chainLev1Id;
    }

    public void setChainLev1Id(UUID chainLev1Id)
    {
        this.chainLev1Id = chainLev1Id;
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    public UUID getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId)
    {
        this.applicationId = applicationId;
    }

    public UUID getStateId()
    {
        return stateId;
    }

    public void setStateId(UUID stateId)
    {
        this.stateId = stateId;
    }

    public String getActiveNodeName()
    {
        return activeNodeName;
    }

    public void setActiveNodeName(String activeNodeName)
    {
        this.activeNodeName = activeNodeName;
    }

    public UUID getActiveNodeId()
    {
        return activeNodeId;
    }

    public void setActiveNodeId(UUID activeNodeId)
    {
        this.activeNodeId = activeNodeId;
    }

    public String getPlaceName()
    {
        return placeName;
    }

    public void setPlaceName(String placeName)
    {
        this.placeName = placeName;
    }

    public UUID getPlaceId()
    {
        return placeId;
    }

    public void setPlaceId(UUID placeId)
    {
        this.placeId = placeId;
    }

    public String getExecutionNodeName()
    {
        return executionNodeName;
    }

    public void setExecutionNodeName(String executionNodeName)
    {
        this.executionNodeName = executionNodeName;
    }

    public UUID getExecutionNodeId()
    {
        return executionNodeId;
    }

    public void setExecutionNodeId(UUID executionNodeId)
    {
        this.executionNodeId = executionNodeId;
    }

    public String getDns()
    {
        return dns;
    }

    public void setDns(String dns)
    {
        this.dns = dns;
    }

    public String getOsAccount()
    {
        return osAccount;
    }

    public void setOsAccount(String osAccount)
    {
        this.osAccount = osAccount;
    }

    public String getWhatWasRun()
    {
        return whatWasRun;
    }

    public void setWhatWasRun(String whatWasRun)
    {
        this.whatWasRun = whatWasRun;
    }

    public String getShortLog()
    {
        return shortLog;
    }

    public void setShortLog(String shortLog)
    {
        this.shortLog = shortLog;
    }

    public int getResultCode()
    {
        return resultCode;
    }

    public void setResultCode(int resultCode)
    {
        this.resultCode = resultCode;
    }

    public DateTime getEnteredPipeAt()
    {
        return enteredPipeAt;
    }

    public void setEnteredPipeAt(DateTime enteredPipeAt)
    {
        this.enteredPipeAt = enteredPipeAt;
    }

    public DateTime getMarkedForUnAt()
    {
        return markedForUnAt;
    }

    public void setMarkedForUnAt(DateTime markedForUnAt)
    {
        this.markedForUnAt = markedForUnAt;
    }

    public DateTime getBeganRunningAt()
    {
        return beganRunningAt;
    }

    public void setBeganRunningAt(DateTime beganRunningAt)
    {
        this.beganRunningAt = beganRunningAt;
    }

    public DateTime getStoppedRunningAt()
    {
        return stoppedRunningAt;
    }

    public void setStoppedRunningAt(DateTime stoppedRunningAt)
    {
        this.stoppedRunningAt = stoppedRunningAt;
    }

    public long getDataIn()
    {
        return dataIn;
    }

    public void setDataIn(long dataIn)
    {
        this.dataIn = dataIn;
    }

    public long getDataOut()
    {
        return dataOut;
    }

    public void setDataOut(long dataOut)
    {
        this.dataOut = dataOut;
    }

    public long getSequence()
    {
        return sequence;
    }

    public void setSequence(long sequence)
    {
        this.sequence = sequence;
    }

    public String getCalendarName()
    {
        return calendarName;
    }

    public void setCalendarName(String calendarName)
    {
        this.calendarName = calendarName;
    }

    public String getCalendarOccurrence()
    {
        return calendarOccurrence;
    }

    public void setCalendarOccurrence(String calendarOccurrence)
    {
        this.calendarOccurrence = calendarOccurrence;
    }

    public String getLogPath()
    {
        return logPath;
    }

    public void setLogPath(String logPath)
    {
        this.logPath = logPath;
    }

    public Boolean getVisible()
    {
        return visible;
    }

    public void setVisible(Boolean visible)
    {
        this.visible = visible;
    }

    public UUID getChainLaunchId()
    {
        return chainLaunchId;
    }

    public void setChainLaunchId(UUID chainLaunchId)
    {
        this.chainLaunchId = chainLaunchId;
    }

    public DateTime getLastLocallyModified()
    {
        return lastLocallyModified;
    }

    public void setLastLocallyModified(DateTime lastLocallyModified)
    {
        this.lastLocallyModified = lastLocallyModified;
    }

    public void insertOrUpdate(Connection conn)
    {
        long count = conn.createQuery("SELECT COUNT(1) FROM RunLog WHERE id=:id").addParameter("id", this.id).executeScalar(Long.class);
        if (count > 0)
        {
            conn.createQuery("UPDATE RunLog SET beganRunningAt=:beganRunningAt, chainLaunchId=:chainLaunchId, "
                    + "chainLev1Id=:chainLev1Id, dataIn=:dataIn, dataOut=:dataOut, lastKnownStatus=:lastKnownStatus, "
                    + "lastLocallyModified=:lastLocallyModified, osAccount=:osAccount, resultCode=:resultCode, "
                    + "sequence=:sequence, shortLog=:shortLog, stoppedRunningAt=:stoppedRunningAt, "
                    + "visible=:visible, whatWasRun=:whatWasRun  WHERE id=:id").bind(this).executeUpdate();
        }
        else
        {
            conn.createQuery("INSERT INTO RunLog (id, visible, applicationId, chainId, stateId, activeNodeId, "
                    + "chainLev1Id, executionNodeId, placeId, chainLaunchId, chainName, applicationName, "
                    + "chainLev1Name, activeNodeName, placeName, executionNodeName, dns, osAccount, "
                    + "whatWasRun, resultCode, lastKnownStatus, shortLog, logPath, dataIn, dataOut, calendarName, "
                    + "calendarOccurrence, sequence, enteredPipeAt, markedForUnAt, beganRunningAt, stoppedRunningAt, "
                    + "lastLocallyModified)" + "VALUES (:id, :visible, :applicationId, :chainId, :stateId, :activeNodeId, "
                    + ":chainLev1Id, :executionNodeId, :placeId, :chainLaunchId, :chainName, :applicationName, "
                    + ":chainLev1Name, :activeNodeName, :placeName, :executionNodeName, :dns, :osAccount, "
                    + ":whatWasRun, :resultCode, :lastKnownStatus, :shortLog, :logPath, :dataIn, :dataOut, :calendarName, "
                    + ":calendarOccurrence, :sequence, :enteredPipeAt, :markedForUnAt, :beganRunningAt, :stoppedRunningAt, "
                    + ":lastLocallyModified)").bind(this).executeUpdate();
        }
    }
}
