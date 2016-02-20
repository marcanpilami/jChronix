package org.chronix.integration.tests;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.nio.file.Paths;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.oxymore.chronix.chain.dto.DTOChain;
import org.oxymores.chronix.core.engine.api.ChronixEngine;
import org.oxymores.chronix.core.engine.api.DTOApplication2;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.EventSource;
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

    @Configuration
    public Option[] config()
    {
        return options(junitBundles(), systemPackage("sun.misc"),
                systemProperty("logback.configurationFile")
                        .value("file:" + Paths.get("./target/test-classes/logback.xml").toAbsolutePath().normalize().toString()),
                mavenBundle("org.slf4j", "slf4j-api", "1.7.14"), mavenBundle("org.slf4j", "log4j-over-slf4j", "1.7.14"),
                mavenBundle("ch.qos.logback", "logback-core", "1.1.3"), mavenBundle("ch.qos.logback", "logback-classic", "1.1.3"),
                // systemProperty("felix.cm.dir").value(Paths.get("./target/test-classes/config").toAbsolutePath().normalize().toString()),
                // mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.8.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"),
                mavenBundle("org.oxymores.chronix", "chronix-source-chain", "0.9.2-SNAPSHOT"),
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

    // TODO: auto create an environment on startup with empty metabase.
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

        // Environment
        DTOEnvironment envt = meta.createMinimalEnvironment(); // node is called "local"
        meta.saveEnvironmentDraft(envt);

        // Application
        DTOApplication2 app = meta.createMinimalApplication(); // also creates placegroup "local"
        app.setName("test app");
        app.setDescription("This app was created by integration tests");

        // Application content
        DTOChain c = new DTOChain("first chain", "integration test chain", app.getGroup("local"));
        c.connect(c.getStart(), c.getEnd());
        app.addEventSource(c);
        // app.addEventSource(new DTOChainStart());
        // app.addEventSource(new DTOChainEnd());

        DTOChain p = new DTOChain("plan", "integration test plan", app.getGroup("local"));
        DTOState s = p.addState(c, app.getGroup("local"));
        app.addEventSource(p);

        // Deploy
        envt.getPlace("local").addMemberOfGroup(app.getGroup("local").getId());
        meta.saveEnvironmentDraft(envt);
        meta.promoteEnvironmentDraft("test commit");

        meta.saveApplicationDraft(app);
        meta.promoteApplicationDraft(app.getId(), "test commit");

        // Tests
        meta.resetCache();
        DTOApplication2 a2 = meta.getApplication(app.getId());
        Assert.assertEquals("test app", a2.getName());

        Assert.assertEquals(4, a2.getEventSources().size());
        boolean found = false;
        for (EventSource d : a2.getEventSources())
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
        order.orderLaunch(a2.getId(), s.getId(), envt.getPlace("local").getId(), true);

        Thread.sleep(3000);
        e.stop();
    }
}
