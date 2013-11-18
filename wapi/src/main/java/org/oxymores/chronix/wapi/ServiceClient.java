/**
 * @author Marc-Antoine Gouillart
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

package org.oxymores.chronix.wapi;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.demo.PlanBuilder;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOApplicationShort;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.Frontier;
import org.oxymores.chronix.dto.Frontier2;
import org.oxymores.chronix.engine.SenderHelpers;
import org.oxymores.chronix.internalapi.IServiceClient;

public class ServiceClient implements IServiceClient
{
	private static Logger log = Logger.getLogger(ChronixContext.class);
	private ChronixContext ctx;

	public ServiceClient(ChronixContext ctx)
	{
		this.ctx = ctx;
	}

	@Override
	public String sayHello()
	{
		log.debug("Ping service was called");
		return "houba hop";
	}

	@Override
	public DTOApplication getApplication(String name)
	{
		log.debug(String.format("getApplication service was called for app name %s", name));
		String id = ctx.applicationsByName.get(name).getId().toString();
		return getApplicationById(id);
	};

	@Override
	public DTOApplication getApplicationById(String id)
	{
		log.debug(String.format("getApplication service was called for app id %s", id));
		Application a = ctx.applicationsById.get(UUID.fromString(id));

		DTOApplication d = Frontier.getApplication(a);
		log.debug("End of getApplication call. Returning an application.");
		return d;
	}

	@Override
	public void stageApplication(DTOApplication app)
	{
		// TODO Replace test code with true persistence
		log.debug("stageApplication service was called");

		Application a = Frontier2.getApplication(app);

		// Put the working copy in the local cache (no impact on engine, different cache)
		this.ctx.applicationsById.put(a.getId(), a);
		this.ctx.applicationsByName.put(a.getName(), a);

		try
		{
			ctx.saveApplication(a);
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("End of stageApplication call.");
	}

	@Override
	public void storeApplication(String uuid)
	{
		log.debug("storeApplication service was called");

		try
		{
			SenderHelpers.sendApplicationToAllClients(this.ctx.applicationsById.get(UUID.fromString(uuid)), ctx);
		} catch (JMSException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.debug("End of storeApplication call.");
	}

	@Override
	public void resetStage()
	{
		// TODO Auto-generated method stub
		log.debug("resetStage service was called");
		log.debug("End of resetStage call.");
	}

	@Override
	public List<Date> getNextRRuleOccurrences(DTORRule rule, String lowerBound, String higherBound)
	{
		ClockRRule r = Frontier2.getRRule(rule);
		Clock tmp = new Clock();
		tmp.addRRuleADD(r);
		PeriodList pl = null;

		DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
		DateTime start = DateTime.parse(lowerBound, df);
		DateTime end = DateTime.parse(higherBound, df);

		try
		{
			pl = tmp.getOccurrences(start, end);
		} catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayList<Date> res = new ArrayList<Date>();
		for (Object pe : pl)
		{
			Period p = (Period) pe;
			res.add(p.getStart());
		}

		return res;
	}

	@Override
	public List<DTOApplicationShort> getAllApplications()
	{
		ArrayList<DTOApplicationShort> res = new ArrayList<DTOApplicationShort>();

		for (Application a : this.ctx.applicationsById.values())
		{
			DTOApplicationShort t = new DTOApplicationShort();
			t.description = a.getDescription();
			t.id = a.getId().toString();
			t.name = a.getName();
			res.add(t);
		}
		return res;
	}

	@Override
	public DTOApplication createApplication(String name, String description)
	{
		Application a = PlanBuilder.buildApplication(name, description);
		PlanBuilder.buildDefaultLocalNetwork(a);
		PlanBuilder.buildShellCommand(a, "echo 'first command'", "first shell command", "a demo command that you can delete");
		ClockRRule r = PlanBuilder.buildRRuleWeekDays(a);
		PlanBuilder.buildClock(a, "once a week day", "day clock", r);

		return Frontier.getApplication(a);
	}
}
