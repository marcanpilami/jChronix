package org.oxymores.chronix.engine.helpers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.jms.JMSException;
import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;

import org.joda.time.DateTime;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.RunnerCommand;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOApplicationShort;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.dto.DTOResultClock;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.PeriodList;

/**
 * The Plan Metadata API implementation as an OSGI Declarative Service.<br>
 * See {@link PlanAccessService} for API details.
 */
@Component
public class ApiPlanAccess implements PlanAccessService
{
    private static final Logger log = LoggerFactory.getLogger(PlanAccessService.class);

    // The service works under an independent context.
    private ChronixContext ctx;

    @Activate
    private void activate(ComponentContext cc)
    {
        // TODO: use configuration. Especially, we need a correct LOCAL NODE for JMS connections to work.
        ctx = new ChronixContext("local", "C:\\TEMP\\db1", false, "C:\\TEMP\\db1\\db_history\\db", "C:\\TEMP\\db1\\db_transac\\db");
    }

    public ApiPlanAccess()
    {
        // Default constructor for OSGI injection
    }

    public ApiPlanAccess(ChronixContext ctx)
    {
        // Specific constructor for non-OSGI environments
        this.ctx = ctx;
    }

    @Override
    public DTOApplication createMinimalApplication()
    {
        Application a = PlanBuilder.buildApplication("new application", "");
        a.createStarterGroups(this.ctx.getEnvironment());
        PlanBuilder.buildShellCommand(a, "echo 'first command'", "first shell command", "a demo command that you can delete");
        ClockRRule r = PlanBuilder.buildRRuleWeekDays(a);
        PlanBuilder.buildClock(a, "once a week day", "day clock", r);
        PlanBuilder.buildChain(a, "first chain", "plan", a.getGroupsList().get(0));

        return CoreToDto.getApplication(a);
    }

    @Override
    public DTOApplication createTestApplication()
    {
        Application a = DemoApplication.getNewDemoApplication();
        a.setname("test application");
        a.createStarterGroups(this.ctx.getEnvironment());
        PlaceGroup pgLocal = a.getGroupsList().get(0);
        Chain c = PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);

        ClockRRule rr1 = PlanBuilder.buildRRuleSeconds(a, 200);
        External ex = PlanBuilder.buildExternal(a, "External");
        Clock ck1 = PlanBuilder.buildClock(a, "every 10 second", "every 10 second", rr1);
        ck1.setDURATION(0);
        RunnerCommand sc1 = PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        RunnerCommand sc2 = PlanBuilder.buildPowerShellCommand(a, "echooooooo bb", "bb", "should display 'bb'");
        RunnerCommand sc3 = PlanBuilder.buildPowerShellCommand(a, "echo fin", "FIN", "should display 'fin'");

        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
        PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");

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

        return CoreToDto.getApplication(a);
    }

    @Override
    public List<DTOApplicationShort> getAllApplications()
    {
        ArrayList<DTOApplicationShort> res = new ArrayList<>();

        for (Application a : this.ctx.getStagedApplications())
        {
            DTOApplicationShort t = new DTOApplicationShort();
            t.setDescription(a.getDescription());
            t.setId(a.getId().toString());
            t.setName(a.getName());
            t.setVersion(a.getVersion());
            t.setLatestVersionComment(a.getCommitComment());
            t.setDraft(!a.isFromCurrentFile());
            t.setLatestSave(a.getLatestSave().toDate());
            res.add(t);
        }
        return res;
    }

    @Override
    public DTOApplication getApplication(String id)
    {
        Application a = this.ctx.getStagedApplication(UUID.fromString(id));
        return CoreToDto.getApplication(a);
    }

    @Override
    public DTOEnvironment getEnvironment()
    {
        return CoreToDto.getEnvironment(this.ctx.getEnvironment());
    }

    @Override
    public DTOResultClock testRecurrenceRule(DTORRule rule)
    {
        DTOResultClock res = new DTOResultClock();
        ClockRRule r = DtoToCore.getRRule(rule);
        Clock tmp = new Clock();
        tmp.addRRuleADD(r);
        PeriodList pl = null;

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
            res.getRes().add(p.getStart());
        }

        return res;
    }

    @Override
    public void resetApplicationDraft(DTOApplication app)
    {
        this.ctx.unstageApplication(DtoToCore.getApplication(app));
    }

    @Override
    public void saveApplicationDraft(DTOApplication app)
    {
        Application a = DtoToCore.getApplication(app);
        try
        {
            this.ctx.stageApplication(a);
        }
        catch (ChronixPlanStorageException e)
        {
            log.error("Could not stage the application", e);
        }
    }

    @Override
    public void promoteApplicationDraft(String id)
    {
        Application a = this.ctx.getApplication(id);
        a.addVersion(a.getVersion() + 1, a.getLatestSave() + "");

        try
        {
            SenderHelpers.sendApplicationToAllClients(a, this.ctx);
        }
        catch (JMSException e)
        {
            log.error("Could not send the application to its clients", e);
        }
        // Do not remove the draft file... otherwise we would have sync issues (ops above are asynchronous).
        this.ctx.stageApplication(a); // Store it with new version number and possible changes.
    }

    @Override
    public void saveEnvironment(DTOEnvironment e)
    {
        try
        {
            Environment env = DtoToCore.getEnvironment(e);
            this.ctx.saveEnvironment(env);
            this.ctx.setEnvironment(env);
            SenderHelpers.sendEnvironmentToAllNodes(env, this.ctx);
        }
        catch (ChronixPlanStorageException | JMSException ex)
        {
            log.error("Could not store environment", ex);
        }
    }

    @Override
    public List<DTOValidationError> validateApplication(DTOApplication app)
    {
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

    @Override
    public List<DTOValidationError> validateEnvironment(DTOEnvironment nn)
    {
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