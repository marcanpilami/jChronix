package org.oxymores.chronix.main;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.TimeZone;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.planbuilder.DemoApplication;
import org.oxymores.chronix.planbuilder.PlanBuilder;
import org.oxymores.chronix.wapi.JettyServer;

public class Scheduler
{
    public static void main(String[] args) throws Exception
    {
        // Scheduler only uses & stores UTC dates.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Load properties
        Properties applicationProps = new Properties();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler.properties");
        applicationProps.load(in);
        in.close();

        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = "localhost";
        }

        boolean startEngine = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.engine", "true"));
        boolean startWS = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.webserver", "true"));
        // boolean startRunner = Boolean.parseBoolean(applicationProps.getProperty("jChronix.startup.runner", "true"));
        String repoPath = applicationProps.getProperty("jChronix.repository.path", ".");
        String mainDataInterface = applicationProps.getProperty("jChronix.engine.mainDataInteface", hostname);
        int mainDataPort = Integer.parseInt(applicationProps.getProperty("jChronix.engine.mainDataPort", "1789"));
        String transacUnit = applicationProps.getProperty("jChronix.engine.transacPersistenceUnitName", "TransacUnit");
        String historyUnit = applicationProps.getProperty("jChronix.engine.historyPersistenceUnitName", "HistoryUnit");
        int nbRunner = Integer.parseInt(applicationProps.getProperty("jChronix.runner.maxjobs", "10"));

        // /////////////////////
        // DEBUG
        // Application a = DemoApplication.getNewDemoApplication(mainDataInterface, mainDataPort);
        Application a = DemoApplication.getNewDemoApplication();
        a.setname("test app");
        PlaceGroup pgLocal = PlanBuilder.buildDefaultLocalNetwork(a, mainDataPort, mainDataInterface);
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

        ChronixContext ctx = new ChronixContext();
        ctx.configurationDirectory = new File(repoPath);
        ChronixEngine tmp = new ChronixEngine(repoPath, mainDataInterface + ":" + mainDataPort);
        tmp.emptyDb();
        ctx.saveApplication(a);
        ctx.setWorkingAsCurrent(a);

        // END DEBUG
        // /////////////////////

        // Engine
        ChronixEngine e = null;
        if (startEngine)
        {
            e = new ChronixEngine(repoPath, mainDataInterface + ":" + mainDataPort, transacUnit, historyUnit, false, nbRunner);
            e.start();
            e.waitForInitEnd();
        }

        // Web server
        if (startWS)
        {
            JettyServer server = new JettyServer(e.getContext());
            server.start();
        }

        SenderHelpers.sendOrderExternalEvent(s1.getId(), null, pgLocal.getPlaces().get(0).getNode(), e.getContext());

        Thread.sleep(1000000); // 1000s, debug
    }
}
