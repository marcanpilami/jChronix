/**
 * @author Marc-Antoine Gouillart
 *
 * See the NOTICE file distributed with this work for information regarding
 * copyright ownership. This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.oxymores.chronix.wapi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
import javax.ws.rs.core.MediaType;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOApplicationShort;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTOResultClock;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.slf4j.LoggerFactory;

/**
 * Handles all the metadata related services. JSON only.
 */
@Path("/meta")
public class ServiceMeta
{

    private static final Logger log = LoggerFactory.getLogger(ServiceMeta.class);
    private final ChronixContext ctx;

    public ServiceMeta(ChronixContext ctx)
    {
        this.ctx = ctx;
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello()
    {
        log.debug("Ping service was called");
        return "houba hop";
    }

    @GET
    @Path("environment")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOEnvironment getEnvironment()
    {
        log.debug("getEnvironment was called");
        return CoreToDto.getEnvironment(ctx.getEnvironment());
    }

    @POST
    @Path("environment")
    @Consumes(MediaType.APPLICATION_JSON)
    public void storeEnvironment(DTOEnvironment n)
    {
        log.debug("storeEnvironment was called");
        try
        {
            Environment env = DtoToCore.getEnvironment(n);
            ctx.saveEnvironment(env);
            ctx.setEnvironment(env);
            SenderHelpers.sendEnvironmentToAllNodes(env, ctx);
        }
        catch (ChronixPlanStorageException | JMSException ex)
        {
            log.error("Could not store environment", ex);
        }
    }

    @GET
    @Path("app/{appid}")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOApplication getApplication(@PathParam("appid") String id)
    {
        log.debug(String.format("getApplication service was called for app id %s", id));
        Application a = ctx.getStagedApplication(UUID.fromString(id));

        DTOApplication d = CoreToDto.getApplication(a);
        log.debug("End of getApplication call. Returning an application.");
        return d;
    }

    @POST
    @Path("app")
    @Consumes(MediaType.APPLICATION_JSON)
    public void stageApplication(DTOApplication app)
    {
        log.debug("stageApplication service was called");

        // Read application
        Application a = DtoToCore.getApplication(app);
        a.isFromCurrentFile(false);
        try
        {
            ctx.stageApplication(a);
        }
        catch (ChronixPlanStorageException e)
        {
            log.error("Could not stage the application", e);
        }
        log.debug("End of stageApplication call.");
    }

    @POST
    @Path("liveapp")
    @Consumes(MediaType.APPLICATION_JSON)
    public void storeApplication(DTOApplication app)
    {
        log.debug("storeApplication service was called");
        app.setVersion(app.getVersion() + 1);
        Application a = DtoToCore.getApplication(app);
        try
        {
            SenderHelpers.sendApplicationToAllClients(a, ctx);
        }
        catch (JMSException e)
        {
            log.error("Could not send the application to its clients", e);
        }
        // Do not remove the working file... otherwise we would have sync issues (ops above are asynchronous).
        stageApplication(app); // Store it with new version number and possible changes.
        log.debug("End of storeApplication call.");
    }

    @POST
    @Path("xappunstage")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetStage(DTOApplication app)
    {
        log.debug("resetStage service was called");
        ctx.unstageApplication(DtoToCore.getApplication(app));
        log.debug("End of resetStage call.");
    }

    @POST
    @Path("rrule/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public DTOResultClock getNextRRuleOccurrences(DTORRule rule)
    {
        DTOResultClock res = new DTOResultClock();
        ClockRRule r = DtoToCore.getRRule(rule);
        Clock tmp = new Clock();
        tmp.addRRuleADD(r);
        PeriodList pl = null;

        DateTimeFormatter df = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
        try
        {
            pl = tmp.getOccurrences(new DateTime(rule.getSimulStart()), new DateTime(rule.getSimulEnd()));
        }
        catch (ParseException e)
        {
            log.error("Could not compute next ocurrences", e);
        }

        for (Object pe : pl)
        {
            Period p = (Period) pe;
            DateFormat dfo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
            res.getRes().add(p.getStart());
        }

        return res;
    }

    @GET
    @Path("app")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DTOApplicationShort> getAllApplications()
    {
        log.debug("getAllApplications service was called");
        ArrayList<DTOApplicationShort> res = new ArrayList<>();

        for (Application a : this.ctx.getStagedApplications())
        {
            DTOApplicationShort t = new DTOApplicationShort();
            t.setDescription(a.getDescription());
            t.setId(a.getId().toString());
            t.setName(a.getName());
            t.setVersion(a.getVersion());
            t.setDraft(!a.isFromCurrentFile());
            t.setLatestSave(a.getLatestSave().toDate());
            res.add(t);
        }
        return res;
    }

    @GET
    @Path("newapp")
    @Produces(MediaType.APPLICATION_JSON)
    public DTOApplication createApplication()
    {
        // Create app (leave incorrect description to force user to set it before saving the app)
        Application a = PlanBuilder.buildApplication("new app", "");
        a.createStarterGroups(this.ctx.getEnvironment());
        PlanBuilder.buildShellCommand(a, "echo 'first command'", "first shell command", "a demo command that you can delete");
        ClockRRule r = PlanBuilder.buildRRuleWeekDays(a);
        PlanBuilder.buildClock(a, "once a week day", "day clock", r);
        PlanBuilder.buildChain(a, "first chain", "plan", a.getGroupsList().get(0));

        return CoreToDto.getApplication(a);
    }

    @POST
    @Path("app/newdemo")
    @Produces(MediaType.APPLICATION_JSON)
    public void createTestApplication()
    {
        Application a = DemoApplication.getNewDemoApplication();
        a.setname("test app");
        a.createStarterGroups(ctx.getEnvironment());
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
        }
        catch (ChronixPlanStorageException e1)
        {
            // DEBUG code, so no need for pretty exc handling
            log.error("{}", e1);
            System.exit(1);
        }
    }

    @POST
    @Path("app/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<DTOValidationError> validateApp(DTOApplication app)
    {
        log.debug("validateApp service was called");
        List<DTOValidationError> res = new ArrayList<>();
        DTOValidationError tmp;

        if (app == null)
        {
            tmp = new DTOValidationError();
            tmp.setErrorMessage("Application sent could not be deserialized. Please check its format.");
            res.add(tmp);
            return res;
        }

        // Read application
        Application a = DtoToCore.getApplication(app);

        // Validate & translate results for the GUI
        res = getErrors(ChronixContext.validate(a));
        return res;
    }

    @POST
    @Path("environment/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public List<DTOValidationError> validateEnvironment(DTOEnvironment nn)
    {
        log.debug("validateEnvironment service was called");
        List<DTOValidationError> res = new ArrayList<>();
        DTOValidationError tmp;

        if (nn == null)
        {
            tmp = new DTOValidationError();
            tmp.setErrorMessage("Environment could not be deserialized. Please check its format.");
            res.add(tmp);
            return res;
        }

        // Read application
        Environment n = DtoToCore.getEnvironment(nn);

        // Validate & translate results for the GUI
        res = getErrors(ChronixContext.validate(n));
        return res;
    }

    private <T> List<DTOValidationError> getErrors(Set<ConstraintViolation<T>> violations)
    {
        DTOValidationError tmp;
        List<DTOValidationError> res = new ArrayList<>();

        for (ConstraintViolation<T> err : violations)
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
