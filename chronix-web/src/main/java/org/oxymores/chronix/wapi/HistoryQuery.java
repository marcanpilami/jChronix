package org.oxymores.chronix.wapi;

import java.util.Date;
import java.util.List;
import org.oxymores.chronix.dto.DTORunLog;

public class HistoryQuery
{
    private Date startedBefore, startedAfter, markedForRunBefore, markedForRunAfter;
    private List<DTORunLog> res;
    private Integer startLine = 0, pageSize = 100;

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
}
