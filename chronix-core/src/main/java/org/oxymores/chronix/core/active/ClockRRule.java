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
package org.oxymores.chronix.core.active;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import net.fortuna.ical4j.model.Recur;

import org.slf4j.Logger;
import org.oxymores.chronix.core.NamedApplicationObject;
import org.slf4j.LoggerFactory;

public class ClockRRule extends NamedApplicationObject
{
    private static final long serialVersionUID = 1092625083715354537L;
    private static Logger log = LoggerFactory.getLogger(ClockRRule.class);

    String period = "DAILY";
    Integer interval = 1;

    @NotNull
    @Size(min = 0, max = 255)
    String bySecond = "", byMinute = "", byHour = "", byDay = "", byMonthDay = "", byMonth = "", byYear = "", bySetPos = "";

    // ///////////////////////////////////////////////////////
    // iCal functions
    public String getICalString()
    {
        String res = "";

        // PERIOD
        res += "FREQ=" + period + ";" + "INTERVAL=" + interval + ";";

        // BYxxxxxx
        res += normalizeByElement(bySecond, "BYSECOND");
        res += normalizeByElement(byMinute, "BYMINUTE");
        res += normalizeByElement(byHour, "BYHOUR");
        res += normalizeByElement(byDay, "BYDAY");
        res += normalizeByElement(byMonthDay, "BYMONTHDAY");
        res += normalizeByElement(byMonth, "BYMONTH");
        res += normalizeByElement(byYear, "BYYEAR");

        res += normalizeByElement(bySetPos, "BYSETPOS");
        // Remove last ';'
        res = res.substring(0, res.length() - 1);

        log.debug("iCal rec string is: " + res);
        return res;
    }

    private String normalizeByElement(String elt, String name)
    {
        String tmp = "";
        if (elt != null && !"".equals(elt) && elt.length() > 0)
        {
            if (",".equals(elt.substring(elt.length() - 1)))
            {
                tmp = elt.substring(0, elt.length() - 1);
            }
            else
            {
                tmp = elt;
            }
            tmp = name + "=" + tmp + ";";
        }
        return tmp;
    }

    public Recur getRecur() throws ParseException
    {
        Recur res = new Recur(this.getICalString());
        // null and -1: Forever
        res.setUntil(null);
        res.setCount(-1);
        return res;
    }

    // iCal functions
    // ///////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////
    // Not so stupid GET/SET
    public void setPeriod(String period)
    {
        List<String> allowed = Arrays.asList(Recur.DAILY, Recur.HOURLY, Recur.MINUTELY, Recur.MONTHLY, Recur.SECONDLY, Recur.WEEKLY,
                Recur.YEARLY);
        if (allowed.contains(period))
        {
            this.period = period;
        }
        else
        {
            this.period = Recur.DAILY;
        }
    }

    public String getPeriod()
    {
        return this.period;
    }

    // Not so stupid GET/SET
    // ///////////////////////////////////////////////////////
    // ///////////////////////////////////////////////////////
    // Stupid GET/SET
    public Integer getINTERVAL()
    {
        return interval;
    }

    public void setINTERVAL(Integer iNTERVAL)
    {
        interval = iNTERVAL;
    }

    public String getBYSECOND()
    {
        return bySecond;
    }

    public void setBYSECOND(String bYSECOND)
    {
        bySecond = bYSECOND;
    }

    public String getBYMINUTE()
    {
        return byMinute;
    }

    public void setBYMINUTE(String bYMINUTE)
    {
        byMinute = bYMINUTE;
    }

    public String getBYHOUR()
    {
        return byHour;
    }

    public void setBYHOUR(String bYHOUR)
    {
        log.debug("DEBUG BYHOUR SET : " + bYHOUR);
        byHour = bYHOUR;
    }

    public String getBYDAY()
    {
        return byDay;
    }

    public void setBYDAY(String bYDAY)
    {
        byDay = bYDAY;
    }

    public String getBYMONTHDAY()
    {
        return byMonthDay;
    }

    public void setBYMONTHDAY(String bYMONTHDAY)
    {
        byMonthDay = bYMONTHDAY;
    }

    public String getBYMONTH()
    {
        return byMonth;
    }

    public void setBYMONTH(String bYMONTH)
    {
        byMonth = bYMONTH;
    }

    public String getBYYEAR()
    {
        return byYear;
    }

    public void setBYYEAR(String bYYEAR)
    {
        byYear = bYYEAR;
    }

    public String getBYSETPOS()
    {
        return bySetPos;
    }

    public void setBYSETPOS(String bYSETPOS)
    {
        bySetPos = bYSETPOS;
    }

    // Stupid GET/SET
    // ///////////////////////////////////////////////////////
}
