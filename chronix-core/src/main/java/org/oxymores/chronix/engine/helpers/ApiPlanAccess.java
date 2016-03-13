package org.oxymores.chronix.engine.helpers;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.validation.ConstraintViolation;
import javax.validation.Path.Node;

import org.apache.commons.lang.NotImplementedException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ContextHandler;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.engine.api.DTOApplicationShort;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.exceptions.ChronixException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Plan Metadata API implementation as an OSGI Declarative Service.<br>
 * See {@link PlanAccessService} for API details.
 */
@Component
public class ApiPlanAccess implements PlanAccessService
{
    private static final Logger log = LoggerFactory.getLogger(PlanAccessService.class);

    private String ctxMetaPath;

    @Activate
    private void activate(ComponentContext cc)
    {
        ctxMetaPath = "C:\\TEMP\\db1";
    }

    private ChronixContextMeta getMetaDb()
    {
        return ContextHandler.getMeta(ctxMetaPath);
    }

    @Override
    public DTOApplication createMinimalApplication()
    {
        DTOApplication app = new DTOApplication();
        app.setId(UUID.randomUUID());

        // Create one group per place inside the environment
        for (DTOPlace p : this.getEnvironment().getPlaces())
        {
            DTOPlaceGroup pg = new DTOPlaceGroup();
            pg.setId(UUID.randomUUID());
            pg.setDescription(p.getName());
            pg.setName(p.getName());

            app.getGroups().add(pg);
        }

        return app;
    }

    @Override
    public DTOApplication createTestApplication()
    {
        throw new NotImplementedException();
        /*
         * Application a = DemoApplication.getNewDemoApplication(); a.setname("test application");
         * a.createStarterGroups(this.ctx.getEnvironment()); PlaceGroup pgLocal = a.getGroupsList().get(0); Chain c =
         * PlanBuilder.buildChain(a, "chain1", "chain1", pgLocal);
         * 
         * ClockRRule rr1 = PlanBuilder.buildRRuleSeconds(a, 200); External ex = PlanBuilder.buildExternal(a, "External"); Clock ck1 =
         * PlanBuilder.buildClock(a, "every 10 second", "every 10 second", rr1); ck1.setDURATION(0); RunnerCommand sc1 =
         * PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'"); RunnerCommand sc2 =
         * PlanBuilder.buildPowerShellCommand(a, "echooooooo bb", "bb", "should display 'bb'"); RunnerCommand sc3 =
         * PlanBuilder.buildPowerShellCommand(a, "echo fin", "FIN", "should display 'fin'");
         * 
         * PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'"); PlanBuilder.buildPowerShellCommand(a, "echo aa",
         * "aa", "should display 'aa'"); PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
         * PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'"); PlanBuilder.buildPowerShellCommand(a, "echo aa",
         * "aa", "should display 'aa'"); PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'");
         * PlanBuilder.buildPowerShellCommand(a, "echo aa", "aa", "should display 'aa'"); PlanBuilder.buildPowerShellCommand(a, "echo aa",
         * "aa", "should display 'aa'");
         * 
         * PlanBuilder.buildExternal(a, "file 1", "/tmp/meuh.txt"); PlanBuilder.buildRRuleMinutes(a, 10); PlanBuilder.buildRRuleMinutes(a,
         * 20); PlanBuilder.buildRRuleMinutes(a, 30);
         * 
         * State s1 = PlanBuilder.buildState(c, pgLocal, ex); State s2 = PlanBuilder.buildState(c, pgLocal, sc1); State s3 =
         * PlanBuilder.buildState(c, pgLocal, sc2); State s4 = PlanBuilder.buildStateAND(c, pgLocal); State s5 = PlanBuilder.buildState(c,
         * pgLocal, sc3); s1.connectTo(s2); s1.connectTo(s3); s2.connectTo(s4); s3.connectTo(s4, 0); s4.connectTo(s5);
         * 
         * return CoreToDto.getApplication(a);
         */
    }

    private DTOApplicationShort appToDtoShort(Application a)
    {
        DTOApplicationShort t = new DTOApplicationShort();
        t.setDescription(a.getDescription());
        t.setId(a.getId().toString());
        t.setName(a.getName());
        t.setVersion(a.getVersion());
        t.setLatestVersionComment(a.getCommitComment());
        t.setLatestSave(a.getLatestSave().toDate());

        return t;
    }

    @Override
    public List<DTOApplicationShort> getAllApplications()
    {
        Map<UUID, DTOApplicationShort> res = new HashMap<>();

        for (Application a : this.getMetaDb().getApplications())
        {
            DTOApplicationShort s = appToDtoShort(a);
            s.setDraft(false);
            res.put(a.getId(), s);
        }
        for (Application a : this.getMetaDb().getDrafts())
        {
            DTOApplicationShort s = appToDtoShort(a);
            s.setDraft(true);
            res.put(a.getId(), s);
        }
        return new ArrayList<>(res.values());
    }

    @Override
    public DTOApplication getApplication(UUID id)
    {
        boolean active = false;
        Application app = this.getMetaDb().getApplicationDraft(id);
        if (app == null)
        {
            active = true;
            app = this.getMetaDb().getApplication(id);
            if (app == null)
            {
                throw new ChronixException("cannot find application " + id);
            }
        }

        DTOApplication res = new DTOApplication();
        res.setActive(active);
        res.setDescription(app.getDescription());
        res.setEventSources(app.getEventSources());
        res.setId(app.getId());
        res.setLatestVersionComment(app.getCommitComment());
        res.setName(app.getName());
        res.setVersion(app.getVersion());

        for (PlaceGroup pg : app.getGroupsList())
        {
            DTOPlaceGroup d = CoreToDto.getPlaceGroup(pg);
            res.getGroups().add(d);
        }

        return res;
    }

    @Override
    public DTOEnvironment getEnvironment()
    {
        Environment e = this.getMetaDb().getEnvironmentDraft();
        if (e == null)
        {
            e = this.getMetaDb().getEnvironment();
        }
        if (e == null)
        {
            throw new IllegalStateException("trying to load an environment from an empty metabase");
        }
        return CoreToDto.getEnvironment(e);
    }

    /*
     * @Override public DTOResultClock testRecurrenceRule(DTORRule rule) { DTOResultClock res = new DTOResultClock(); ClockRRule r =
     * DtoToCore.getRRule(rule); Clock tmp = new Clock(); tmp.addRRuleADD(r); PeriodList pl = null;
     * 
     * try { pl = tmp.getOccurrences(new DateTime(rule.getSimulStart()), new DateTime(rule.getSimulEnd())); } catch (ParseException e) {
     * log.error("Could not compute next ocurrences", e); }
     * 
     * for (Object pe : pl) { Period p = (Period) pe; res.getRes().add(p.getStart()); }
     * 
     * return res; }
     */

    @Override
    public void resetApplicationDraft(DTOApplication app)
    {
        this.getMetaDb().resetApplicationDraft(app.getId());
    }

    @Override
    public void saveApplicationDraft(DTOApplication app)
    {
        Application a = new Application();
        a.setDescription(app.getDescription());
        a.setName(app.getName());
        a.setId(app.getId());

        for (EventSource d : app.getEventSources())
        {
            a.registerSource(d, FrameworkUtil.getBundle(d.getClass()).getSymbolicName());
        }

        for (DTOPlaceGroup pg : app.getGroups())
        {
            PlaceGroup gr = DtoToCore.getPlaceGroup(pg, getMetaDb().getEnvironment());
            a.addGroup(gr);
        }

        this.getMetaDb().saveApplicationDraft(a);
    }

    @Override
    public void promoteApplicationDraft(UUID id, String commitMessage)
    {
        // TODO: send via JMS, not via FS
        this.getMetaDb().activateApplicationDraft(id, commitMessage);

        /*
         * Application a = this.ctx.getApplication(id); a.addVersion(a.getVersion() + 1, a.getLatestSave() + "");
         * 
         * try { SenderHelpers.sendApplicationToAllClients(a, this.ctx); } catch (JMSException e) { log.error(
         * "Could not send the application to its clients", e); } // Do not remove the draft file... otherwise we would have sync issues
         * (ops above are asynchronous). this.ctx.stageApplication(a); // Store it with new version number and possible changes.
         */
    }

    @Override
    public void saveEnvironmentDraft(DTOEnvironment e)
    {
        Environment env = DtoToCore.getEnvironment(e);
        this.getMetaDb().saveEnvironmentDraft(env);
    }

    @Override
    public void promoteEnvironmentDraft(String commitMessage)
    {
        // TODO: JMS send
        this.getMetaDb().activateEnvironmentDraft(commitMessage);
    }

    @Override
    public DTOEnvironment createMinimalEnvironment()
    {
        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = "localhost";
        }

        Environment n = new Environment();

        // Execution node (the local sever)
        ExecutionNode n1 = new ExecutionNode();
        n1.setName("local");
        ExecutionNodeConnectionAmq conn = new ExecutionNodeConnectionAmq();
        conn.setDns(hostname);
        conn.setqPort(1789);
        n1.addConnectionMethod(conn);
        n1.setX(100);
        n1.setY(100);

        // One place on that node
        Place p1 = new Place();
        p1.setName("local");
        p1.setNode(n1);
        n.addPlace(p1);

        n.addNode(n1);
        n.setConsole(n1);

        return CoreToDto.getEnvironment(n);
    }

    @Override
    public List<DTOValidationError> validateApplication(DTOApplication app)
    {
        throw new NotImplementedException();
        /*
         * List<DTOValidationError> res = new ArrayList<>(); DTOValidationError tmp;
         * 
         * if (app == null) { tmp = new DTOValidationError(); tmp.setErrorMessage(
         * "Application sent could not be deserialized. Please check its format."); res.add(tmp); return res; }
         * 
         * // Read application Application a = DtoToCore.getApplication(app);
         * 
         * // Validate & translate results for the GUI res = getErrors(ChronixContext.validate(a)); return res;
         */
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
        // res = getErrors(ChronixContext.validate(n));
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

    @Override
    public void resetCache()
    {
        ContextHandler.resetCtx();
    }
}