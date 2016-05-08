package org.oxymores.chronix.dto;

import java.util.Date;
import java.util.UUID;

import javax.xml.bind.annotation.XmlElement;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "DTORunLog")
public class DTORunLog
{
    UUID id;
    String lastKnownStatus;
    @XmlElement(nillable = true)
    String calendarName, calendarOccurrence;
    String chainName;
    String chainLev1Name;
    String applicationName;
    String activeNodeName;
    String placeName;
    String executionNodeName;
    String dns;
    String osAccount;
    String whatWasRun;
    String chainLaunchId;
    String shortLog;
    int resultCode;

    @XmlElement(nillable = true)
    Date enteredPipeAt, markedForRunAt, beganRunningAt, stoppedRunningAt;

    long dataIn, dataOut;
    long sequence;

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getLastKnownStatus()
    {
        return lastKnownStatus;
    }

    public void setLastKnownStatus(String lastKnownStatus)
    {
        this.lastKnownStatus = lastKnownStatus;
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

    public String getChainName()
    {
        return chainName;
    }

    public void setChainName(String chainName)
    {
        this.chainName = chainName;
    }

    public String getChainLev1Name()
    {
        return chainLev1Name;
    }

    public void setChainLev1Name(String chainLev1Name)
    {
        this.chainLev1Name = chainLev1Name;
    }

    public String getApplicationName()
    {
        return applicationName;
    }

    public void setApplicationName(String applicationName)
    {
        this.applicationName = applicationName;
    }

    public String getActiveNodeName()
    {
        return activeNodeName;
    }

    public void setActiveNodeName(String activeNodeName)
    {
        this.activeNodeName = activeNodeName;
    }

    public String getPlaceName()
    {
        return placeName;
    }

    public void setPlaceName(String placeName)
    {
        this.placeName = placeName;
    }

    public String getExecutionNodeName()
    {
        return executionNodeName;
    }

    public void setExecutionNodeName(String executionNodeName)
    {
        this.executionNodeName = executionNodeName;
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

    public Date getMarkedForRunAt()
    {
        return markedForRunAt;
    }

    public void setMarkedForRunAt(Date markedForRunAt)
    {
        this.markedForRunAt = markedForRunAt;
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

    public String getChainLaunchId()
    {
        return this.chainLaunchId;
    }

    public void setChainLaunchId(String a)
    {
        this.chainLaunchId = a;
    }

    public void setShortLog(String log)
    {
        this.shortLog = log;
    }

    public String getShortLog()
    {
        return this.shortLog;
    }
}
