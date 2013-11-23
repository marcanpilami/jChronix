/**
 * By Marc-Antoine Gouillart, 2012
 * 
 * See the NOTICE file distributed with this work for 
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file 
 * except in compliance with the License. You may obtain 
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oxymores.chronix.core.timedata;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.lang.time.DateFormatUtils;

@Entity
public class RunLog implements Serializable
{
    private static final long serialVersionUID = 154654512882124L;

    @Column(length = 20)
    private String lastKnownStatus;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    @Id
    private String id; // UUID (is the id of the pipelinejob)
    private String chainName;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String chainId; // UUID
    private String chainLev1Name;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String chainLev1Id; // UUID
    private String applicationName;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String applicationId; // UUID
    @Column(length = 36)
    private String stateId; // UUID
    private String activeNodeName;
    @Column(length = 36)
    private String activeNodeId; // UUID
    private String placeName;
    @Column(length = 36)
    private String placeId; // UUID
    private String executionNodeName;
    @Column(length = 36)
    private String executionNodeId; // UUID
    private String dns;
    private String osAccount;
    private String whatWasRun;
    @Column(length = 10000)
    private String shortLog;
    private int resultCode;
    private Date enteredPipeAt;
    private Date markedForUnAt;
    private Date beganRunningAt;
    private Date stoppedRunningAt;
    private long dataIn, dataOut;
    private long sequence;
    private String calendarName;
    private String calendarOccurrence;
    private String logPath;
    private Boolean visible = true;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    private String chainLaunchId;
    private Date lastLocallyModified;

    public static String getTitle()
    {
        String res = "";
        res = String
                .format("%-36s | %-20s | %-20s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %-14s | %-14s | %-14s | %-14s | %-15s | %-15s | %-10s | %-5s | %36s",
                        "ID", "placename", "execnodename", "chainName", "applicationName", "activeNodeName", "osAccount", "whatWasRun",
                        "RC", "enteredPipeAt ", "beganRunningAt", "stoppedRunning", "markedForUnAt ", "calendarName", "calendar occr",
                        "logPath", "visib", "chainLaunchId");
        return res;
    }

    public String getLine()
    {
        String res = "";
        res = String
                .format("%36s | %-20s | %-20s | %-20s | %-20s | %-20s | %-10s | %-30s | %-3s | %-14s | %-14s | %-14s | %-14s | %-15s | %-15s | %-10s | %-5s | %36s",
                        this.id, this.placeName.substring(0, Math.min(19, placeName.length())), this.executionNodeName, chainName
                                .substring(0, Math.min(19, chainName.length())), applicationName.substring(0,
                                Math.min(19, applicationName.length())),
                        activeNodeName.substring(0, Math.min(19, activeNodeName.length())), osAccount,
                        whatWasRun == null ? "" : whatWasRun.substring(0, Math.min(29, whatWasRun.length())), resultCode,
                        enteredPipeAt == null ? null : DateFormatUtils.format(enteredPipeAt, "dd/MM HH:mm:ss"),
                        beganRunningAt == null ? null : DateFormatUtils.format(beganRunningAt, "dd/MM HH:mm:ss"),
                        stoppedRunningAt == null ? null : DateFormatUtils.format(stoppedRunningAt, "dd/MM HH:mm:ss"),
                        markedForUnAt == null ? null : DateFormatUtils.format(markedForUnAt, "dd/MM HH:mm:ss"), calendarName == null ? null
                                : calendarName.substring(0, Math.min(14, calendarName.length())), calendarOccurrence == null ? null
                                : calendarOccurrence.substring(0, Math.min(19, calendarOccurrence.length())), logPath == null ? null
                                : logPath.substring(0, Math.min(9, logPath.length())), visible, chainLaunchId);
        return res;
    }

    public String getLastKnownStatus()
    {
        return lastKnownStatus;
    }

    public void setLastKnownStatus(String lastKnownStatus)
    {
        this.lastKnownStatus = lastKnownStatus;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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

    public String getChainId()
    {
        return chainId;
    }

    public void setChainId(String chainId)
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

    public String getChainLev1Id()
    {
        return chainLev1Id;
    }

    public void setChainLev1Id(String chainLev1Id)
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

    public String getApplicationId()
    {
        return applicationId;
    }

    public void setApplicationId(String applicationId)
    {
        this.applicationId = applicationId;
    }

    public String getStateId()
    {
        return stateId;
    }

    public void setStateId(String stateId)
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

    public String getActiveNodeId()
    {
        return activeNodeId;
    }

    public void setActiveNodeId(String activeNodeId)
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

    public String getPlaceId()
    {
        return placeId;
    }

    public void setPlaceId(String placeId)
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

    public String getExecutionNodeId()
    {
        return executionNodeId;
    }

    public void setExecutionNodeId(String executionNodeId)
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

    public Date getEnteredPipeAt()
    {
        return enteredPipeAt;
    }

    public void setEnteredPipeAt(Date enteredPipeAt)
    {
        this.enteredPipeAt = enteredPipeAt;
    }

    public Date getMarkedForUnAt()
    {
        return markedForUnAt;
    }

    public void setMarkedForUnAt(Date markedForUnAt)
    {
        this.markedForUnAt = markedForUnAt;
    }

    public Date getBeganRunningAt()
    {
        return beganRunningAt;
    }

    public void setBeganRunningAt(Date beganRunningAt)
    {
        this.beganRunningAt = beganRunningAt;
    }

    public Date getStoppedRunningAt()
    {
        return stoppedRunningAt;
    }

    public void setStoppedRunningAt(Date stoppedRunningAt)
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

    public String getChainLaunchId()
    {
        return chainLaunchId;
    }

    public void setChainLaunchId(String chainLaunchId)
    {
        this.chainLaunchId = chainLaunchId;
    }

    public Date getLastLocallyModified()
    {
        return lastLocallyModified;
    }

    public void setLastLocallyModified(Date lastLocallyModified)
    {
        this.lastLocallyModified = lastLocallyModified;
    }
}
