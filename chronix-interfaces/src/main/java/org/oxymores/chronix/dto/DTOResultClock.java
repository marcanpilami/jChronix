package org.oxymores.chronix.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DTOResultClock
{
    private List<Date> res = new ArrayList<>();

    public List<Date> getRes()
    {
        return res;
    }

    public void setRes(List<Date> res)
    {
        this.res = res;
    }
}
