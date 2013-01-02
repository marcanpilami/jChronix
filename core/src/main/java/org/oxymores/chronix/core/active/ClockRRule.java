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

import net.fortuna.ical4j.model.Recur;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ApplicationObject;

public class ClockRRule extends ApplicationObject
{
	private static final long serialVersionUID = 1092625083715354537L;
	private static Logger log = Logger.getLogger(ClockRRule.class);

	String PERIOD = "DAILY";
	Integer INTERVAL = 1;

	String Name, Description;

	String BYSECOND = "", BYMINUTE = "", BYHOUR = "", BYDAY = "", BYMONTHDAY = "", BYMONTH = "", BYYEAR = "", BYSETPOS = "";

	// ///////////////////////////////////////////////////////
	// iCal functions
	public String getICalString()
	{
		String res = "";// "RRULE:";

		// PERIOD
		res += "FREQ=" + PERIOD + ";" + "INTERVAL=" + INTERVAL + ";";

		// BYxxxxxx
		res += normalizeByElement(BYSECOND, "BYSECOND");
		res += normalizeByElement(BYMINUTE, "BYMINUTE");
		res += normalizeByElement(BYHOUR, "BYHOUR");
		res += normalizeByElement(BYDAY, "BYDAY");
		res += normalizeByElement(BYMONTHDAY, "BYMONTHDAY");
		res += normalizeByElement(BYMONTH, "BYMONTH");
		res += normalizeByElement(BYYEAR, "BYYEAR");

		res += normalizeByElement(BYSETPOS, "BYSETPOS");
		res = res.substring(0, res.length() - 1); // Remove last ';'

		log.debug("iCal rec string is: " + res);
		return res;
	}

	private String normalizeByElement(String elt, String name)
	{
		String tmp = "";
		if (elt != null && elt != "" && elt.length() > 0)
		{
			if (elt.substring(elt.length() - 1).equals(","))
				tmp = elt.substring(0, elt.length() - 1);
			else
				tmp = elt;
			tmp = name + "=" + tmp + ";";
		}
		return tmp;
	}

	public Recur getRecur() throws ParseException
	{
		Recur res = new Recur(this.getICalString());
		res.setUntil(null); // Forever
		res.setCount(-1); // Forever
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
			this.PERIOD = period;
		else
			this.PERIOD = Recur.DAILY;
	}

	public String getPeriod()
	{
		return this.PERIOD;
	}

	// Not so stupid GET/SET
	// ///////////////////////////////////////////////////////

	// ///////////////////////////////////////////////////////
	// Stupid GET/SET
	public String getName()
	{
		return Name;
	}

	public void setName(String name)
	{
		Name = name;
	}

	public String getDescription()
	{
		return Description;
	}

	public void setDescription(String description)
	{
		Description = description;
	}

	public Integer getINTERVAL()
	{
		return INTERVAL;
	}

	public void setINTERVAL(Integer iNTERVAL)
	{
		INTERVAL = iNTERVAL;
	}

	public String getBYSECOND()
	{
		return BYSECOND;
	}

	public void setBYSECOND(String bYSECOND)
	{
		BYSECOND = bYSECOND;
	}

	public String getBYMINUTE()
	{
		return BYMINUTE;
	}

	public void setBYMINUTE(String bYMINUTE)
	{
		BYMINUTE = bYMINUTE;
	}

	public String getBYHOUR()
	{
		return BYHOUR;
	}

	public void setBYHOUR(String bYHOUR)
	{
		log.debug("DEBUG BYHOUR SET : " + bYHOUR);
		BYHOUR = bYHOUR;
	}

	public String getBYDAY()
	{
		return BYDAY;
	}

	public void setBYDAY(String bYDAY)
	{
		BYDAY = bYDAY;
	}

	public String getBYMONTHDAY()
	{
		return BYMONTHDAY;
	}

	public void setBYMONTHDAY(String bYMONTHDAY)
	{
		BYMONTHDAY = bYMONTHDAY;
	}

	public String getBYMONTH()
	{
		return BYMONTH;
	}

	public void setBYMONTH(String bYMONTH)
	{
		BYMONTH = bYMONTH;
	}

	public String getBYYEAR()
	{
		return BYYEAR;
	}

	public void setBYYEAR(String bYYEAR)
	{
		BYYEAR = bYYEAR;
	}

	public String getBYSETPOS()
	{
		return BYSETPOS;
	}

	public void setBYSETPOS(String bYSETPOS)
	{
		BYSETPOS = bYSETPOS;
	}

	// Stupid GET/SET
	// ///////////////////////////////////////////////////////
}
