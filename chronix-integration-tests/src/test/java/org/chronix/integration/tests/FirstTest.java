package org.chronix.integration.tests;

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
import org.oxymores.chronix.core.engine.api.PlanAccessService;
import org.oxymores.chronix.core.source.api.DTO;
import org.oxymores.chronix.core.source.api.EventSourceBehaviour;
import org.oxymores.chronix.dto.DTOEnvironment;

import static org.ops4j.pax.exam.CoreOptions.*;

import java.util.UUID;

import javax.inject.Inject;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class FirstTest
{
    @Inject
    ChronixEngine e;

    @Inject
    PlanAccessService meta;

    @Configuration
    public Option[] config()
    {
        return options(mavenBundle("org.apache.felix", "org.apache.felix.scr", "2.0.2"),
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
                mavenBundle("org.jboss.logging", "jboss-logging", "3.3.0.Final"),
                /* mavenJar("org.slf4j", "slf4j-simple", "1.7.13"), mavenBundle("org.slf4j", "slf4j-api", "1.7.13"), */
                mavenBundle("javax.validation", "validation-api", "1.1.0.Final"),
                /* mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.8.5"), */
                mavenBundle("javax.annotation", "javax.annotation-api", "1.2"), mavenBundle("org.ow2.asm", "asm", "5.0.4"),
                mavenBundle("org.apache.aries.blueprint", "org.apache.aries.blueprint.core", "1.5.0"),
                mavenBundle("org.apache.aries.proxy", "org.apache.aries.proxy.api", "1.0.1"),
                mavenBundle("org.apache.aries", "org.apache.aries.util", "1.1.1"));
    }

    @Test
    public void testStartStop()
    {
        Assert.assertNotNull(e);
        e.start();
        e.stop();
    }

    @Test
    public void testCreatePlan()
    {
        Assert.assertNotNull(meta);

        DTOEnvironment envt = meta.createMinimalEnvironment();
        meta.saveEnvironmentDraft(envt);
        meta.promoteEnvironmentDraft("test commit");

        DTOApplication2 app = meta.createMinimalApplication();
        app.setName("test app");
        app.setDescription("This app was created by integration tests");

        DTOChain c = new DTOChain("first chain", "integration test chain");
        app.addEventSource(c);

        meta.saveApplicationDraft(app);
        meta.promoteApplicationDraft(app.getId(), "test commit");

        meta.resetCache();

        DTOApplication2 a2 = meta.getApplication(app.getId());
        Assert.assertEquals("test app", a2.getName());

        Assert.assertEquals(3, a2.getEventSources().size());
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
    }
}
