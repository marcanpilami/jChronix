package org.chronix.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.service.cm.ConfigurationAdmin;
import org.oxymore.chronix.chain.dto.DTOChain;
import org.oxymore.chronix.chain.dto.DTOChainEnd;
import org.oxymore.chronix.chain.dto.DTOChainStart;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.oxymores.chronix.core.engine.api.DTOApplication2;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.dto.DTOEnvironment;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FirstTest
{
    @Inject
    ChronixEngine e;

    @Inject
    PlanAccessService meta;

    @Inject
    OrderService order;

    // @Inject
    // LogService log;

    @Configuration
    public Option[] config()
    {
        return options(/*
                        * mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.8.5"), mavenBundle("org.ops4j.pax.logging",
                        * "pax-logging-api", "1.8.5"), mavenBundle("org.ops4j.pax.logging", "pax-logging-log4j2", "1.8.5"),
                        * mavenBundle("org.ops4j.pax.confman", "pax-confman-propsloader", "0.2.3"),
                        */
                // systemProperty("felix.cm.dir").value(Paths.get("./target/test-classes/config").toAbsolutePath().normalize().toString()),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"),
                /* mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.8"), */
                mavenBundle("org.hsqldb", "hsqldb", "2.3.3").startLevel(1), bundle("wrap:mvn:org.hsqldb/sqltool/2.3.3"),
                mavenBundle("org.oxymores.chronix", "chronix-source-chain", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-core", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-nonosgilibs", "0.9.2-SNAPSHOT"),
                mavenBundle("org.oxymores.chronix", "chronix-plugin-api", "0.9.2-SNAPSHOT"),
                mavenBundle("com.thoughtworks.xstream", "xstream", "1.4.8"), mavenBundle("joda-time", "joda-time", "2.8.1"),
                systemPackage("sun.misc"), systemPackage("javax.sql"), systemPackage("sun.reflect"), junitBundles(),
                systemPackage("javax.annotation"), mavenBundle("org.apache.activemq", "activemq-osgi", "5.13.0").noStart(),
                mavenBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec", "1.0.1"),
                mavenBundle("org.apache.geronimo.specs", "geronimo-jms_1.1_spec", "1.1.1"),
                mavenBundle("com.fasterxml", "classmate", "1.1.0"), mavenBundle("commons-codec", "commons-codec", "1.8"),
                mavenBundle("commons-io", "commons-io", "2.4"), mavenBundle("commons-lang", "commons-lang", "2.6"),
                mavenBundle("commons-logging", "commons-logging", "1.1.3"), mavenBundle("commons-net", "commons-net", "3.3"),
                mavenBundle("org.fusesource.hawtbuf", "hawtbuf", "1.11"),
                mavenBundle("org.hibernate", "hibernate-validator", "5.2.2.Final"), mavenBundle("org.mnode.ical4j", "ical4j", "1.0.6"),
                mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"), mavenBundle("org.slf4j", "slf4j-api", "1.7.14"),
                mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.14"),
                /* mavenBundle("org.slf4j", "slf4j-log4j12", "1.7.14").noStart(), mavenBundle("log4j", "log4j", "1.2.17"), */
                mavenBundle("ch.qos.logback", "logback-core", "1.1.3"), mavenBundle("ch.qos.logback", "logback-classic", "1.1.3"),
                mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),
                /* mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.8.5"), */
                mavenBundle("javax.annotation", "javax.annotation-api", "1.2"), mavenBundle("org.ow2.asm", "asm", "5.0.4"),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core", "1.5.0"),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api", "1.0.1"),
                mavenBundle("org.apache.aries", "org.apache.aries.util", "1.1.1"), systemProperty("logback.configurationFile")
                        .value("file:" + Paths.get("./target/test-classes/logback.xml").toAbsolutePath().normalize().toString()));

        /*
         * systemProperty("log4j.configuration") .value("file:" + Paths.get(".").toAbsolutePath().normalize().toString() +
         * "/target/test-classes/log4j.properties"), systemProperty("org.ops4j.pax.logging.log4j.config.file") .value("file:" +
         * Paths.get(".").toAbsolutePath().normalize().toString() + "/target/test-classes/log4j.properties")
         */

    }

    // @Before
    /*
     * public void setLog() throws Exception { // System.out.println(FrameworkUtil.getBundle(ConsoleAppender.class).getLocation()); try {
     * Properties p = new Properties(); p.load(this.getClass().getResourceAsStream("/log4j.properties")); Dictionary<String, String> d = new
     * Hashtable<>(); for (Map.Entry<Object, Object> e : p.entrySet()) { d.put((String) e.getKey(), (String) e.getValue()); }
     * 
     * /* for (org.osgi.service.cm.Configuration cc : admin.listConfigurations(null)) { System.out.println(cc.getPid());
     * System.out.println(cc.getProperties()); }
     *//*
       * 
       * org.osgi.service.cm.Configuration cfg = admin.getConfiguration("org.ops4j.pax.logging", null);
       * System.out.println("RRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR"); System.out.println(cfg.getProperties());
       * cfg.setBundleLocation(null); // cfg.update(d); // System.out.println(cfg.getProperties());
       * 
       * // Thread.sleep(5000);
       * 
       * } catch (IOException e) { throw new RuntimeException("could not change log configuration", e); } }
       */

    //TODO: auto create an environment on startup with empty metabase.
    // @Test
    public void testStartStop()
    {
        Assert.assertNotNull(e);
        e.start();
        e.stop();
    }

    @Test
    public void testCreatePlan() throws InterruptedException
    {
        Assert.assertNotNull(meta);

        DTOEnvironment envt = meta.createMinimalEnvironment(); // node is called "local"
        meta.saveEnvironmentDraft(envt);
        meta.promoteEnvironmentDraft("test commit");

        DTOApplication2 app = meta.createMinimalApplication(); // also creates placegroup "local"
        app.setName("test app");
        app.setDescription("This app was created by integration tests");

        DTOChain c = new DTOChain("first chain", "integration test chain", app.getGroup("local"));
        c.connect(c.getStart(), c.getEnd());
        app.addEventSource(c);
        app.addEventSource(new DTOChainStart());
        app.addEventSource(new DTOChainEnd());

        // Deploy
        envt.getPlace("local").addMemberOfGroup(app.getGroup("local").getId());

        meta.saveEnvironmentDraft(envt);
        meta.promoteEnvironmentDraft("test commit");
        meta.saveApplicationDraft(app);
        meta.promoteApplicationDraft(app.getId(), "test commit");

        // meta.resetCache();

        DTOApplication2 a2 = meta.getApplication(app.getId());
        Assert.assertEquals("test app", a2.getName());

        // Assert.assertEquals(3, a2.getEventSources().size());
        boolean found = false;
        for (DTO d : a2.getEventSources())
        {
            if (d instanceof DTOChain && "first chain".equals(((DTOChain) d).getName()))
            {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        e.start();
        System.out.println(c.getStart().getId());
        order.orderLaunch(a2.getId(), c.getStart().getId(), envt.getPlace("local").getId(), true);

        Thread.sleep(3000);
        e.stop();
    }
}
