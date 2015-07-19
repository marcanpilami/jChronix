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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;
import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOApplicationShort;
import org.oxymores.chronix.dto.DTONetwork;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.internalapi.IServiceClient;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;

/**
 Handles all the metadata related services. Both JSON (default) and XML.
 */
@Path("/meta")
public class ServiceClient implements IServiceClient
{
    private static Logger log = Logger.getLogger(ServiceClient.class);
    private ChronixContext ctx;

    public ServiceClient(ChronixContext ctx)
    {
        this.ctx = ctx;
    }

    @Override
    @GET
    @Path("ping")
    @Produces("text/plain")
    public String sayHello()
    {
        log.debug("Ping service was called");
        return "houba hop";
    }

    @Override
    @GET
    @Path("app/name/{appname}")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public DTOApplication getApplication(@PathParam("appname") String name)
    {
        log.debug(String.format("getApplication service was called for app name %s", name));
        String id = ctx.getApplicationByName(name).getId().toString();
        return getApplicationById(id);
    }

    @GET
    @Path("network")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public DTONetwork getNetwork()
    {
        log.debug("getNetwork was called");
        return CoreToDto.getNetwork(ctx.getNetwork());
    }

    @Override
    @GET
    @Path("app/first")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public DTOApplication getFirstApplication()
    {
        log.debug(String.format("getFirstApplication service was called"));
        if (ctx.getApplications().size() > 0)
        {
            return getApplicationById(ctx.getApplications().iterator().next().getId().toString());
        }
        else
        {
            return createApplication("first application", "created automatically");
        }
    }

    @Override
    @GET
    @Path("app/id/{appid}")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public DTOApplication getApplicationById(@PathParam("appid") String id)
    {
        log.debug(String.format("getApplication service was called for app id %s", id));
        Application a = ctx.getApplication(id);

        DTOApplication d = CoreToDto.getApplication(a);
        log.debug("End of getApplication call. Returning an application.");
        return d;
    }

    @Override
    @POST
    @Path("app")
    @Produces(
            {
                "application/json", "application/xml"
            })
    @Consumes(
            {
                "application/json", "application/xml"
            })
    public void stageApplication(DTOApplication app)
    {
        log.debug("stageApplication service was called");

        // Read application
        Application a = DtoToCore.getApplication(app);

        // Put the working copy in the local cache (no impact on engine, different cache)
        this.ctx.addApplicationToCache(a);

        try
        {
            ctx.saveApplication(a);
        }
        catch (ChronixPlanStorageException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.debug("End of stageApplication call.");
    }

    @Override
    @POST
    @Path("app/id/{uuid}/send")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public void storeApplication(@PathParam("uuid") String uuid)
    {
        log.debug("storeApplication service was called");

        try
        {
            SenderHelpers.sendApplicationToAllClients(this.ctx.getApplication(uuid), ctx);
        }
        catch (JMSException e)
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
        ClockRRule r = DtoToCore.getRRule(rule);
        Clock tmp = new Clock();
        tmp.addRRuleADD(r);
        PeriodList pl = null;

        DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
        DateTime start = DateTime.parse(lowerBound, df);
        DateTime end = DateTime.parse(higherBound, df);

        try
        {
            pl = tmp.getOccurrences(start, end);
        }
        catch (ParseException e)
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
    @GET
    @Path("app")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public List<DTOApplicationShort> getAllApplications()
    {
        log.debug("getAllApplications service was called");
        ArrayList<DTOApplicationShort> res = new ArrayList<>();

        for (Application a : this.ctx.getApplications())
        {
            DTOApplicationShort t = new DTOApplicationShort();
            t.setDescription(a.getDescription());
            t.setId(a.getId().toString());
            t.setName(a.getName());
            res.add(t);
        }
        return res;
    }

    @Override
    @POST
    @Path("app/new/{name}/{description}")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public DTOApplication createApplication(@PathParam("name") String name, @PathParam("description") String description)
    {
        // Check if no app of this name
        Application e = this.ctx.getApplicationByName(name);
        if (e != null)
        {
            name = name + "-" + UUID.randomUUID().hashCode();
        }

        // Create app
        Application a = PlanBuilder.buildApplication(name, description);
        a.createStarterGroups(this.ctx.getNetwork());
        PlanBuilder.buildShellCommand(a, "echo 'first command'", "first shell command", "a demo command that you can delete");
        ClockRRule r = PlanBuilder.buildRRuleWeekDays(a);
        PlanBuilder.buildClock(a, "once a week day", "day clock", r);
        PlanBuilder.buildChain(a, "first chain", "plan", a.getGroupsList().get(0));

        return CoreToDto.getApplication(a);
    }

    @POST
    @Path("app/newdemo")
    @Produces(
            {
                "application/json", "application/xml"
            })
    public void createTestApplication()
    {
        Application a = DemoApplication.getNewDemoApplication();
        a.setname("test app");
        a.createStarterGroups(ctx.getNetwork());
        PlaceGroup pgLocal = a.getGroupsList().get(0);
        Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);

        ClockRRule rr1 = PlanBuilder.buildRRuleSeconds(a, 200);
        External ex = PlanBuilder.buildExternal(a, "External");
        Clock ck1 = PlanBuilder.buildClock(a, "every 10 second", "every 10 second", rr1);
        ck1.setDURATION(0);
        ShellCommand sc1 = PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        ShellCommand sc2 = PlanBuilder.buildShellCommand("powershell.exe", a, "echooooooo bb", "bb", "should display 'bb'");
        ShellCommand sc3 = PlanBuilder.buildShellCommand("powershell.exe", a, "echo fin", "FIN", "should display 'fin'");

        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildShellCommand("powershell.exe", a, "echo aa", "aa", "should display 'aa'");

        PlanBuilder.buildExternal(a, "file 1", "/tmp/meuh.txt");
        PlanBuilder.buildRRuleMinutes(a, 10);
        PlanBuilder.buildRRuleMinutes(a, 20);
        PlanBuilder.buildRRuleMinutes(a, 30);

        State s1 = PlanBuilder.buildState(c, pgLocal, ex);
        State s2 = PlanBuilder.buildState(c, pgLocal, sc1);
        State s3 = PlanBuilder.buildState(c, pgLocal, sc2);
        State s4 = PlanBuilder.buildStateAND(c, pgLocal);
        State s5 = PlanBuilder.buildState(c, pgLocal, sc3);
        s1.connectTo(s2);
        s1.connectTo(s3);
        s2.connectTo(s4);
        s3.connectTo(s4, 0);
        s4.connectTo(s5);

        try
        {
            ctx.saveApplication(a);
            ctx.setWorkingAsCurrent(a);
        }
        catch (ChronixPlanStorageException e1)
        {
            // DEBUG code, so no need for pretty exc handling
            e1.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public List<DTOValidationError> validateApp(DTOApplication app)
    {
        List<DTOValidationError> res = new ArrayList<DTOValidationError>();
        DTOValidationError tmp = null;

        // Read application
        Application a = DtoToCore.getApplication(app);

        // Validate & translate results for the GUI
        for (ConstraintViolation<Application> err : ChronixContext.validate(a))
        {
            tmp = new DTOValidationError();
            tmp.setErroneousValue(err.getInvalidValue() == null ? "" : err.getInvalidValue().toString());
            tmp.setErrorMessage(err.getMessage());
            for (Node n : err.getPropertyPath())
            {
                tmp.setErrorPath(n.getName());
            }
            tmp.setItemIdentification(err.getLeafBean().toString());
            tmp.setItemType(err.getLeafBean().getClass().getSimpleName());
            res.add(tmp);
        }

        return res;
    }
}
