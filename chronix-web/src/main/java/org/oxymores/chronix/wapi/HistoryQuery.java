package org.oxymores.chronix.wapi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import org.oxymores.chronix.dto.DTORunLog;

public class HistoryQuery
{
    private Date startedBefore, startedAfter, markedForRunBefore, markedForRunAfter;
    private Integer startLine = 0, pageSize = 100;

    // Sort fields
    @XmlElementWrapper(name = "sortby")
    private List<SortSpec> sorts = new ArrayList<>();

    // Result fields
    private Long totalLogs = null;
    private List<DTORunLog> res;

    // Sort helpers
    public static enum SortOrder
    {
        ASC, DESC;
    }

    public static enum SortColumn
    {
        id("id"),
        applicationName("applicationName"),
        executionNodeName("executionNodeName"),
        placeName("placeName"),
        chainName("chainName"),
        activeNodeName("activeNodeName"),
        lastKnownStatus("lastKnownStatus"),
        markedForRunAt("markedForUnAt"),
        stoppedRunningAt("stoppedRunningAt"),
        chainLaunchId("chainLaunchId");

        private final String coreLogField;

        private SortColumn(String coreLogField)
        {
            this.coreLogField = coreLogField;
        }

        String getCoreLogField()
        {
            return this.coreLogField;
        }
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class SortSpec
    {
        SortOrder order = SortOrder.ASC;
        SortColumn col;

        @SuppressWarnings("unused")
        private SortSpec()
        {
            // Stupid bean convention requires this empty constructor.
        }

        SortSpec(SortOrder order, SortColumn column)
        {
            this.order = order;
            this.col = column;
        }
    }

    // Stupid accessors at the ned of the file
    public Date getStartedBefore()
    {
        return startedBefore;
    }

    public void setStartedBefore(Date startedBefore)
    {
        this.startedBefore = startedBefore;
    }

    public Date getStartedAfter()
    {
        return startedAfter;
    }

    public void setStartedAfter(Date startedAfter)
    {
        this.startedAfter = startedAfter;
    }

    public List<DTORunLog> getRes()
    {
        return res;
    }

    public void setRes(List<DTORunLog> res)
    {
        this.res = res;
    }

    public Date getMarkedForRunAfter()
    {
        return markedForRunAfter;
    }

    public void setMarkedForRunAfter(Date markedForRunAfter)
    {
        this.markedForRunAfter = markedForRunAfter;
    }

    public Date getMarkedForRunBefore()
    {
        return markedForRunBefore;
    }

    public void setMarkedForRunBefore(Date markedForUnBefore)
    {
        this.markedForRunBefore = markedForUnBefore;
    }

    public Integer getStartLine()
    {
        return startLine;
    }

    public void setStartLine(Integer startLine)
    {
        this.startLine = startLine;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }

    public Long getTotalLogs()
    {
        return totalLogs;
    }

    public void setTotalLogs(Long totalLogs)
    {
        this.totalLogs = totalLogs;
    }

    public List<SortSpec> getSorts()
    {
        return sorts;
    }

    protected void setSorts(List<SortSpec> sorts)
    {
        this.sorts = sorts;
    }
}
