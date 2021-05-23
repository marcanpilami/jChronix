package org.oxymores.chronix.core.context.api;

import java.io.File;
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.PlaceGroup;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ContextHandler;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.engine.api.DTOApplicationShort;
import org.oxymores.chronix.core.engine.api.DTOToken;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.DTOFunctionalSequence;
import org.oxymores.chronix.dto.DTOPlace;
import org.oxymores.chronix.dto.DTOPlaceGroup;
import org.oxymores.chronix.dto.DTOValidationError;
import org.oxymores.chronix.exceptions.ChronixException;
import org.oxymores.chronix.exceptions.ChronixInitializationException;

/**
 * The Plan Metadata API implementation as an OSGI Declarative Service.<br>
 * See {@link PlanAccessService} for API details.
 */
@Component(configurationPid = "ApiPlanAccess")
public class ApiPlanAccess implements PlanAccessService
{
    private String ctxMetaPath;

    @Activate
    @Modified
    private void activate(Map<String, String> configuration)
    {
        ctxMetaPath = configuration.getOrDefault("chronix.repository.path", null);
        if (ctxMetaPath != null && !(new File(ctxMetaPath).exists()))
        {
            throw new ChronixInitializationException(
                    "cannot create api service - directory " + ctxMetaPath + " does not exist. Check service configuration.");
        }
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
            DTOPlaceGroup pg = new DTOPlaceGroup(p.getName(), p.getName());
            app.addGroup(pg);
        }

        // Call plugin initialisation
        for (EventSourceProvider service : getMetaDb().getAllKnownSourceProviders())
        {
            service.onNewApplication(app);
        }

        // Done
        return app;
    }

    @Override
    public DTOApplication createTestApplication()
    {
        EventSourceProvider chainPrv = this.getMetaDb().getSourceProvider("org.oxymores.chronix.source.chain.prv.ChainProvider");
        EventSourceProvider planPrv = this.getMetaDb().getSourceProvider("org.oxymores.chronix.source.chain.prv.PlanProvider");
        EventSourceProvider shellPrv = this.getMetaDb().getSourceProvider("org.oxymores.chronix.source.command.prv.ShellCommandProvider");
        ParameterProvider shellPrmPrv = this.getMetaDb().getParameterProvider("org.oxymores.chronix.prm.command.prv.ShellCommandProvider");
        DTOEnvironment envt = this.getEnvironment();

        DTOApplication a = createMinimalApplication();
        a.setName("test application");
        a.setDescription("This app was created for test purposes");

        DTOEventSourceContainer plan1 = new DTOEventSourceContainer(planPrv, a, "plan", "integration test plan", null);

        // Deploy
        envt.getPlace("local").addMemberOfGroup(a.getGroup("local").getId());

        // Some content
        DTOEventSource shell1 = new DTOEventSource(shellPrv, a, "shell1", "shell1").setField("runnerCapability", "shell.wincmd") //
                .setField("COMMAND", "echo") //
                .addParameter("aa").addParameter("bb").addParameter("cc")
                .addParameter(new DTOParameter(null, shellPrmPrv).setField("runnerCapability", "shell.wincmd").setField("COMMAND", "echo")
                        .addAdditionalParameter("dd"))
                .addParameter(new DTOParameter(null, shellPrmPrv)
                        .setField("runnerCapability", "shell.wincmd").setField(new DTOParameter("COMMAND", shellPrmPrv)
                                .setField("runnerCapability", "shell.wincmd").setField("COMMAND", "echo").addAdditionalParameter("echo"))
                        .addAdditionalParameter("ee"));
        a.addEventSource(shell1);

        DTOEventSourceContainer chain1 = new DTOEventSourceContainer(chainPrv, a, "first chain", "integration test chain", null)
                .setAllStates(a.getGroup("local"));
        a.addEventSource(chain1);

        return a;
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
        res.setId(app.getId());
        res.setLatestVersionComment(app.getCommitComment());
        res.setName(app.getName());
        res.setVersion(app.getVersion());

        for (EventSourceDef esw : app.getEventSources().values())
        {
            res.addEventSource(esw.getDTO());
        }

        for (PlaceGroup pg : app.getGroupsList())
        {
            DTOPlaceGroup d = CoreToDto.getPlaceGroup(pg);
            res.addGroup(d);
        }

        for (DTOToken t : app.getTokens().values())
        {
            res.addToken(t);
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

        for (DTOEventSource d : app.getEventSources())
        {
            a.addSource(d, this.getMetaDb());
        }

        for (DTOPlaceGroup pg : app.getGroups())
        {
            PlaceGroup gr = DtoToCore.getPlaceGroup(pg, getMetaDb().getEnvironment());
            a.addGroup(gr);
        }

        for (DTOFunctionalSequence seq : app.getSequences().values())
        {
            a.addCalendar(DtoToCore.getFunctionalSequence(seq));
        }

        for (DTOToken t : app.getTokens())
        {
            a.addToken(t);
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
        throw new UnsupportedOperationException("not implemented");
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