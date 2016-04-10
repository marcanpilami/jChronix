package org.chronix.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
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
    @Inject
    protected BundleContext bc;

    @Inject
    protected ConfigurationAdmin conf;

    @Inject
    protected PlanAccessService meta;

    @Inject
    protected HistoryService history;

    @Inject
    protected OrderService order;

    protected EventSourceProvider chainPrv, planPrv;

    protected DTOEnvironment envt;
    protected DTOApplication app;
    protected DTOEventSource plan1;
    protected DTOEventSource noop;
    protected DTOEventSource and;
    protected DTOEventSource or;
    protected DTOEventSource failure;
    protected Map<String, ChronixEngine> engines;

    protected static String configPath = Paths.get("./target/felix-config").toAbsolutePath().normalize().toString();
    protected static String tmpPath = Paths.get("./target/felix-tmp").toAbsolutePath().normalize().toString();
    protected static String tmpAmqPath = Paths.get("./target/amq-tmp").toAbsolutePath().normalize().toString();
    protected static String nodesPath = Paths.get("./target/nodes").toAbsolutePath().normalize().toString();

    protected static String localNodeMetaPath = Paths.get("./target/nodes/local").toAbsolutePath().normalize().toString();

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
        return options(junitBundles(), systemPackage("sun.misc"),
                systemProperty("logback.configurationFile")
                        .value("file:" + Paths.get("./target/test-classes/logback.xml").toAbsolutePath().normalize().toString()),
                systemProperty("felix.cm.dir").value(configPath), systemProperty("org.osgi.framework.storage").value(tmpPath),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.8"), mavenBundle("org.slf4j", "slf4j-api", "1.7.14"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.14"), mavenBundle("ch.qos.logback", "logback-core", "1.1.3"),
                mavenBundle("ch.qos.logback", "logback-classic", "1.1.3"), mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"),
                mavenBundle("org.oxymores.chronix", "chronix-source-chain", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-source-basic", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-agent-command", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-agent-command-shell", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-core", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-nonosgilibs", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-plugin-api", "0.9.2-SNAPSHOT"), mavenBundle("org.hsqldb", "hsqldb", "2.3.3"),
                mavenBundle("com.thoughtworks.xstream", "xstream", "1.4.8"), mavenBundle("joda-time", "joda-time", "2.8.1"),
                mavenBundle("com.fasterxml", "classmate", "1.1.0"), mavenBundle("commons-codec", "commons-codec", "1.8"),
                mavenBundle("commons-io", "commons-io", "2.4"), mavenBundle("commons-lang", "commons-lang", "2.6"),
                mavenBundle("commons-logging", "commons-logging", "1.1.3"),
                mavenBundle("org.hibernate", "hibernate-validator", "5.2.2.Final"), mavenBundle("org.mnode.ical4j", "ical4j", "1.0.6"),
                mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
                mavenBundle("javax.validation", "validation-api", "1.1.0.Final"));
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

    @Before
    public void before() throws Exception
    {
        // Sanity check
        Assert.assertNotNull(meta);
        Assert.assertNotNull(order);
        Assert.assertNotNull(conf);

        // Factories
        chainPrv = getProvider("org.oxymores.chronix.source.chain.prv.ChainProvider");
        Assert.assertNotNull(chainPrv);
        planPrv = getProvider("org.oxymores.chronix.source.chain.prv.PlanProvider");
        Assert.assertNotNull(planPrv);

        // Clean caches & first node metabase (we actually work inside this metabase for creating the test plan)
        meta.resetCache();
        try
        {
            FileUtils.cleanDirectory(new File(localNodeMetaPath));
        }
        catch (Exception e)
        {
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
        // app.addEventSource(noop);
        plan1 = planPrv.newInstance("plan", "integration test plan", app);

        // base elements
        noop = getProvider("org.oxymores.chronix.source.basic.prv.NoopProvider").newInstance("", "", app);
        and = getProvider("org.oxymores.chronix.source.basic.prv.AndProvider").newInstance("", "", app);
        or = getProvider("org.oxymores.chronix.source.basic.prv.OrProvider").newInstance("", "", app);
        failure = getProvider("org.oxymores.chronix.source.basic.prv.FailureProvider").newInstance("", "", app);

        // Deploy
        envt.getPlace("local").addMemberOfGroup(app.getGroup("local").getId());

        // App is not saved here - it will be by the test

        // Allow the broker to start (and clear remaining messages if any)
        try
        {
            org.osgi.service.cm.Configuration cfg = conf.getConfiguration("ServiceHost", null);
            Dictionary<String, Object> props = (cfg.getProperties() == null ? new Hashtable<String, Object>() : cfg.getProperties());
            props.put("org.oxymores.chronix.network.dbpath", tmpAmqPath);
            props.put("org.oxymores.chronix.network.clear", "true");
            props.put("org.oxymores.chronix.network.nodeid", "3654654");
            cfg.update(props);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @After
    public void after() throws Exception
    {
        // Stop all services after each test (including the host)
        for (org.osgi.service.cm.Configuration cfg : conf.listConfigurations(null))
        {
            if (cfg.getFactoryPid() != null && cfg.getFactoryPid().contains("agent"))
            {
                // These elements self-destruct. No need to remove them.
                continue;
            }
            cfg.delete();
        }

        // Wait for end of all engines
        ServiceTracker<ChronixEngine, ChronixEngine> tracker = new ServiceTracker<>(bc,
                bc.createFilter("(objectClass=" + ChronixEngine.class.getCanonicalName() + ")"), null);
        tracker.open();
        while (tracker.getServices() != null && tracker.getServices().length > 0)
        {
            Thread.sleep(100);
        }
        tracker.close();
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
