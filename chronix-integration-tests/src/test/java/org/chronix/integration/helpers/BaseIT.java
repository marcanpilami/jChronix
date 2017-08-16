package org.chronix.integration.helpers;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.oxymores.chronix.api.agent.MessageListenerService;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOEventSourceContainer;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.EventSourceProvider;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.oxymores.chronix.core.engine.api.DTOApplication;
import org.oxymores.chronix.core.engine.api.HistoryService;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.dto.DTOEnvironment;
import org.oxymores.chronix.dto.HistoryQuery;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BaseIT
{
    @Rule
    public TestName testName = new TestName();

    @Inject
    protected BundleContext bc;

    @Inject
    protected ConfigurationAdmin conf;

    @Inject
    protected PlanAccessService meta;

    @Inject
    protected HistoryService history;

    protected OrderService order;

    protected EventSourceProvider chainPrv, planPrv, shellPrv, setPrv, getPrv, failOnPlacePrv, extPrv, sleepPrv;
    protected ParameterProvider strPrmPrv, shellPrmPrv;

    protected DTOEnvironment envt;
    protected DTOApplication app;
    protected DTOEventSourceContainer plan1;
    protected DTOEventSource noop;
    protected DTOEventSource and;
    protected DTOEventSource or;
    protected DTOEventSource failure;
    protected Map<String, ChronixEngine> engines;

    protected static String configPath = Paths.get("./target/felix-config").toAbsolutePath().normalize().toString();
    protected static String tmpPath = Paths.get("./target/felix-tmp").toAbsolutePath().normalize().toString();
    protected static String tmpAmqPath = Paths.get("./target/amq-tmp").toAbsolutePath().normalize().toString();
    protected static String tmpPaxPath = Paths.get("./target/pax-tmp").toAbsolutePath().normalize().toString();
    protected static String nodesPath = Paths.get("./target/nodes").toAbsolutePath().normalize().toString();

    protected static String localNodeMetaPath = Paths.get("./target/nodes/local").toAbsolutePath().normalize().toString();

    protected static String nl = System.getProperty("line.separator");

    @BeforeClass
    public static void init()
    {
        // Must be set before Felix startup: temporary storage. (Windows users: add AV exception)
        new File(tmpPath).mkdirs();
        System.setProperty("org.osgi.framework.storage", tmpPath);

        // The "local" node metabase is created to allow playing with it before starting any node.
        new File(localNodeMetaPath).mkdirs();

        // No service should start from a remaining configuration
        try
        {
            FileUtils.cleanDirectory(new File(configPath));
        }
        catch (Exception e)
        {
        }
    }

    @Configuration
    public Option[] config() throws IOException
    {
        return options(junitBundles(), systemPackage("sun.misc"), CoreOptions.cleanCaches(), CoreOptions.workingDirectory(tmpPaxPath),
                systemProperty("logback.configurationFile")
                        .value("file:" + Paths.get("./target/test-classes/logback.xml").toAbsolutePath().normalize().toString()),
                systemProperty("felix.cm.dir").value(configPath), systemProperty("felix.cm.loglevel").value("3"),
                frameworkProperty("felix.log.level").value("3"), systemProperty("org.osgi.framework.storage").value(tmpPath),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(), mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.14"),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.scr").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-messaging-amq").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-source-chain").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-source-basic").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-source-test").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-agent-command").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-agent-command-shell").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-core").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-nonosgilibs").versionAsInProject(),
                mavenBundle("org.oxymores.chronix", "chronix-plugin-api").versionAsInProject(),
                mavenBundle("org.hsqldb", "hsqldb").versionAsInProject(),
                mavenBundle("com.thoughtworks.xstream", "xstream").versionAsInProject(),
                mavenBundle("joda-time", "joda-time").versionAsInProject(), mavenBundle("com.fasterxml", "classmate").versionAsInProject(),
                mavenBundle("commons-codec", "commons-codec").versionAsInProject(),
                mavenBundle("commons-io", "commons-io").versionAsInProject(),
                mavenBundle("commons-lang", "commons-lang").versionAsInProject(),
                mavenBundle("commons-logging", "commons-logging").versionAsInProject(),
                mavenBundle("org.hibernate", "hibernate-validator").versionAsInProject(),
                mavenBundle("org.mnode.ical4j", "ical4j").versionAsInProject(),
                mavenBundle("org.jboss.logging", "jboss-logging").versionAsInProject(),
                mavenBundle("javax.validation", "validation-api").versionAsInProject());
    }

    private EventSourceProvider getProvider(String implementationClassName)
    {
        ServiceTracker<EventSourceProvider, EventSourceProvider> st = null;
        try
        {
            st = new ServiceTracker<>(bc, bc.createFilter("(component.name=" + implementationClassName + ")"), null);
            st.open();
            st.waitForService(2000);
            return st.getService();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (st != null)
            {
                st.close();
            }
        }
    }

    private ParameterProvider getParameterProvider(String implementationClassName)
    {
        ServiceTracker<ParameterProvider, ParameterProvider> st = null;
        try
        {
            st = new ServiceTracker<>(bc, bc.createFilter("(component.name=" + implementationClassName + ")"), null);
            st.open();
            st.waitForService(2000);
            return st.getService();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (st != null)
            {
                st.close();
            }
        }
    }

    @Before
    public void before() throws Exception
    {
        System.out.println("**********************************************************");
        System.out.println("Starting test " + testName.getMethodName());

        // Create and start the messaging system. It needs a configuration, so cannot simply be injected in fields above.
        org.osgi.service.cm.Configuration cfg = conf.getConfiguration("ServiceHost", null);
        Dictionary<String, Object> props = (cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties());
        props.put("org.oxymores.chronix.network.dbpath", tmpAmqPath.replace('\\', '/'));
        props.put("org.oxymores.chronix.network.clear", "true");
        props.put("org.oxymores.chronix.network.nodeid", "3654654");
        cfg.update(props);

        ServiceTracker<OrderService, OrderService> st = null;
        try
        {
            st = new ServiceTracker<>(bc, OrderService.class, null);
            st.open();
            st.waitForService(60000);
            order = st.getService();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (st != null)
            {
                st.close();
            }
        }

        ///////////////////////////////////////////////////////////////////////
        // Sanity check
        Assert.assertNotNull(meta);
        Assert.assertNotNull(order);
        Assert.assertNotNull(conf);

        // Factories
        chainPrv = getProvider("org.oxymores.chronix.source.chain.prv.ChainProvider");
        Assert.assertNotNull(chainPrv);
        planPrv = getProvider("org.oxymores.chronix.source.chain.prv.PlanProvider");
        Assert.assertNotNull(planPrv);
        shellPrv = getProvider("org.oxymores.chronix.source.command.prv.ShellCommandProvider");
        setPrv = getProvider("org.oxymores.chronix.source.basic.prv.SetVarProvider");
        getPrv = getProvider("org.oxymores.chronix.source.basic.prv.GetVarProvider");
        failOnPlacePrv = getProvider("org.oxymores.chronix.source.basic.prv.FailOnPlaceProvider");
        extPrv = getProvider("org.oxymores.chronix.source.basic.prv.ExternalProvider");
        sleepPrv = getProvider("org.oxymores.chronix.source.basic.prv.SleepProvider");

        strPrmPrv = getParameterProvider("org.oxymores.chronix.prm.basic.prv.StringParameterProvider");
        shellPrmPrv = getParameterProvider("org.oxymores.chronix.prm.command.prv.ShellCommandProvider");

        // Clean caches & first node metabase (we actually work inside this metabase for creating the test plan)
        meta.resetCache(); // no need - reset is called during engine shutdown
        try
        {
            FileUtils.cleanDirectory(new File(localNodeMetaPath));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        // Maps
        engines = new HashMap<>();

        // Environment
        envt = meta.createMinimalEnvironment(); // node is called "local"
        meta.saveEnvironmentDraft(envt);

        // Application
        app = meta.createMinimalApplication(); // also creates placegroup "local"
        app.setName("test app");
        app.setDescription("This app was created by integration tests");
        plan1 = new DTOEventSourceContainer(planPrv, app, "plan", "integration test plan", null);

        // base elements
        noop = getSingletonSource(app, "org.oxymores.chronix.source.basic.prv.NoopProvider");
        and = getSingletonSource(app, "org.oxymores.chronix.source.basic.prv.AndProvider");
        or = getSingletonSource(app, "org.oxymores.chronix.source.basic.prv.OrProvider");
        failure = getSingletonSource(app, "org.oxymores.chronix.source.basic.prv.FailureProvider");

        // Deploy
        envt.getPlace("local").addMemberOfGroup(app.getGroup("local").getId());

        // App is not saved here - it will be by the test
    }

    private DTOEventSource getSingletonSource(DTOApplication app, String providerClassName)
    {
        for (DTOEventSource s : app.getEventSources())
        {
            if (s.getBehaviourClassName().equals(providerClassName))
            {
                return s;
            }
        }
        throw new RuntimeException("no singleton found for type " + providerClassName);
    }

    @After
    public void after() throws Exception
    {
        System.out.println("**** CLEANUP");

        // List all engines - so that we can wait for their death later
        ServiceTracker<ChronixEngine, ChronixEngine> tracker = new ServiceTracker<>(bc,
                bc.createFilter("(objectClass=" + ChronixEngine.class.getCanonicalName() + ")"), null);
        tracker.open();
        List<ChronixEngine> engines = new ArrayList<>();
        for (ChronixEngine e : tracker.getServices(new ChronixEngine[1]))
        {
            engines.add(e);
        }
        tracker.close();

        /*
         * ServiceTracker<ChronixEngine, ChronixEngine> tracker3 = new ServiceTracker<>(bc, bc.createFilter("(objectClass=*)"), null);
         * tracker3.open(); for (ServiceReference o : tracker3.getServiceReferences()) { System.out.println(Arrays.toString((String[])
         * o.getProperty("objectClass"))); } tracker3.close();
         */

        ServiceTracker<MessageListenerService, MessageListenerService> tracker2 = new ServiceTracker<>(bc,
                bc.createFilter("(objectClass=" + MessageListenerService.class.getCanonicalName() + ")"), null);
        tracker2.open();
        MessageListenerService server = tracker2.waitForService(100000);
        tracker2.close();

        // Stop all services after each test (including the host)
        for (org.osgi.service.cm.Configuration cfg : conf.listConfigurations(null))
        {
            try
            {
                if (cfg.getFactoryPid() != null && cfg.getFactoryPid().contains("agent"))
                {
                    // These elements self-destruct. No need to remove them.
                    continue;
                }
            }
            catch (IllegalStateException e)
            {
                // Already dead, nothing to do!
                continue;
            }
            cfg.delete();
        }

        // Wait for end of all engines
        System.out.println(Thread.currentThread().getName());
        for (ChronixEngine e : engines)
        {
            e.waitShutdown();
        }
        engines.clear();
        server.waitUntilServerIsStopped();

        // displayOsgiBundleInfo();

        System.out.println("End of test " + testName.getMethodName());
        System.out.println("**********************************************************");
    }

    @SuppressWarnings("rawtypes")
    protected void displayOsgiBundleInfo() throws InvalidSyntaxException
    {
        ServiceTracker<ChronixEngine, ChronixEngine> tracker3 = new ServiceTracker<>(bc, bc.createFilter("(objectClass=*)"), null);
        tracker3.open();
        for (ServiceReference o : tracker3.getServiceReferences())
        {
            System.out.println(Arrays.toString((String[]) o.getProperty("objectClass")) + " - " + o.getBundle().getSymbolicName() + " - "
                    + intToState(o.getBundle().getState()));
        }
        System.out.println("**** Bundles");
        for (Bundle b : bc.getBundles())
        {
            System.out.println(b.getSymbolicName() + " - " + intToState(b.getState()) + " uses:");
            for (ServiceReference sr : b.getServicesInUse() != null ? b.getServicesInUse() : new ServiceReference[0])
            {
                System.out.println("     * " + sr);
            }
        }
        tracker3.close();
    }

    private String intToState(int state)
    {
        switch (state)
        {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING3";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "" + state;
        }
    }

    protected void save()
    {
        meta.saveEnvironmentDraft(envt);
        meta.promoteEnvironmentDraft("test commit");

        meta.saveApplicationDraft(app);
        meta.promoteApplicationDraft(app.getId(), "test commit");
    }

    protected void addAndStartEngine(String name)
    {
        addAndStartEngine(name, false);
    }

    protected void addAndStartEngine(String name, boolean purge)
    {
        try
        {
            File nodePath = new File(FilenameUtils.concat(nodesPath, name));
            nodePath.mkdirs();
            if (purge)
            {
                FileUtils.cleanDirectory(nodePath);
            }

            org.osgi.service.cm.Configuration cfg = conf.getConfiguration("scheduler", null);
            Dictionary<String, Object> props = (cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties());
            props.put("chronix.repository.path", nodePath.getAbsolutePath());
            props.put("chronix.cluster.node.name", "local");
            cfg.update(props);

            ServiceTracker<ChronixEngine, ChronixEngine> tracker = new ServiceTracker<>(bc,
                    bc.createFilter(
                            "(&(objectClass=" + ChronixEngine.class.getCanonicalName() + ")(chronix.cluster.node.name=" + name + "))"),
                    null);
            tracker.open();
            tracker.waitForService(20 * 1000);
            ChronixEngine e = tracker.getService();
            tracker.close();
            if (e == null)
            {
                throw new RuntimeException("could not start engine");
            }
            e.waitOperational();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void waitForOk(int count, int maxSeconds)
    {
        waitForOk(count, maxSeconds, 0);
    }

    protected void waitForOk(int count, int maxSeconds, int waitForExcessMs)
    {
        int waitedMs = 0;
        HistoryQuery q = new HistoryQuery();
        q.setResultCode(0);
        history.query(q);
        while (q.getRes().size() < count && waitedMs < maxSeconds * 1000)
        {
            try
            {
                Thread.sleep(50);
                waitedMs += 50;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            history.query(q);
        }
        if (q.getRes().size() == count)
        {
            try
            {
                Thread.sleep(waitForExcessMs);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected void waitForEnded(int count, int maxSeconds, int waitForExcessMs)
    {
        int waitedMs = 0;
        HistoryQuery q = new HistoryQuery();
        q.hasEnded(true);
        history.query(q);
        while (q.getRes().size() < count && waitedMs < maxSeconds * 1000)
        {
            try
            {
                Thread.sleep(50);
                waitedMs += 50;
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            history.query(q);
        }
        if (q.getRes().size() == count)
        {
            try
            {
                Thread.sleep(waitForExcessMs);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    protected void checkHistory(int nbOk, int nbKo)
    {
        HistoryQuery q = new HistoryQuery();

        q.setResultCode(0);
        history.query(q);
        int ok = q.getRes().size();
        Assert.assertEquals(nbOk, ok);

        q.setResultCode(null);
        history.query(q);
        int all = q.getRes().size();
        Assert.assertEquals(nbKo, all - ok);
    }

    protected DTOState getChainStart(DTOEventSourceContainer c)
    {
        for (DTOState s : c.getContainedStates())
        {
            if (s.getEventSourceId().equals(UUID.fromString("647594b0-498f-4042-933f-855682095c6c")))
            {
                return s;
            }
        }
        throw new RuntimeException("invalid chain");
    }

    protected DTOState getChainEnd(DTOEventSourceContainer c)
    {
        for (DTOState s : c.getContainedStates())
        {
            if (s.getEventSourceId().equals(UUID.fromString("8235272c-b78d-4350-a887-aed0dcdfb215")))
            {
                return s;
            }
        }
        throw new RuntimeException("invalid chain");
    }
}
