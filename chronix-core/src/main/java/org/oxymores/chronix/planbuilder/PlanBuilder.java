/**
 * By Marc-Antoine Gouillart, 2012
 *
 * See the NOTICE file distributed with this work for
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.oxymores.chronix.planbuilder;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.fortuna.ical4j.model.Recur;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.ExecutionNodeConnectionAmq;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.active.And;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.RunnerCommand;
import org.oxymores.chronix.engine.modularity.runner.RunnerConstants;

public final class PlanBuilder
{
    private static final String HOSTNAME_FALLBACK = "localhost";

    private PlanBuilder()
    {

    }

    public static Application buildApplication(String name, String description)
    {
        Application a = new Application();
        a.setname(name);
        a.setDescription(description);

        return a;
    }

    public static Chain buildChain(Application a, String name, String description, PlaceGroup targets)
    {
        Chain c1 = new Chain();
        c1.setDescription(description);
        c1.setName(name);
        a.addActiveElement(c1);

        // Start & end retrieval
        ChainStart cs = null;
        for (ActiveNodeBase nb : a.getActiveElements().values())
        {
            if (nb instanceof ChainStart)
            {
                cs = (ChainStart) nb;
            }
        }
        ChainEnd ce = null;
        for (ActiveNodeBase nb : a.getActiveElements().values())
        {
            if (nb instanceof ChainEnd)
            {
                ce = (ChainEnd) nb;
            }
        }

        // Start
        State s1 = new State();
        s1.setChain(c1);
        s1.setRunsOn(targets);
        s1.setRepresents(cs);
        s1.setX(100);
        s1.setY(100);

        // End
        State s2 = new State();
        s2.setChain(c1);
        s2.setRunsOn(targets);
        s2.setRepresents(ce);
        s2.setX(300);
        s2.setY(200);

        return c1;
    }

    public static Chain buildPlan(Application a, String name, String description)
    {
        Chain c1 = new Chain();
        c1.setDescription(description);
        c1.setName(name);
        a.addActiveElement(c1);
        return c1;
    }

    public static ExecutionNode buildExecutionNode(Environment a, String name, int port)
    {
        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = HOSTNAME_FALLBACK;
        }
        return buildExecutionNode(a, name, hostname, port);
    }

    public static ExecutionNode buildExecutionNode(Environment a, String name, String dns, int port)
    {
        return buildExecutionNode(a, name, dns, port, 100, 100);
    }

    public static ExecutionNode buildExecutionNode(Environment a, String name, String dns, int port, int x, int y)
    {
        ExecutionNode n1 = new ExecutionNode();
        n1.setName(name);
        ExecutionNodeConnectionAmq conn = new ExecutionNodeConnectionAmq();
        conn.setDns(dns);
        conn.setqPort(port);
        n1.addConnectionMethod(conn);
        n1.setX(x);
        n1.setY(y);
        a.addNode(n1);

        return n1;
    }

    public static Place buildPlace(Environment a, String name, ExecutionNode en)
    {
        Place p1 = new Place();
        p1.setName(name);
        p1.setNode(en);
        a.addPlace(p1);

        return p1;
    }

    public static PlaceGroup buildPlaceGroup(Application a, String name, String description, Place... places)
    {
        PlaceGroup pg1 = new PlaceGroup();
        pg1.setDescription(description);
        pg1.setName(name);
        a.addGroup(pg1);

        for (Place p : places)
        {
            pg1.addPlace(p);
        }

        return pg1;
    }

    public static Environment buildLocalDnsNetwork()
    {
        return buildLocalDnsNetwork(1789);
    }

    public static Environment buildLocalDnsNetwork(int port)
    {
        // Execution node (the local sever)
        String hostname;
        try
        {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (UnknownHostException e)
        {
            hostname = HOSTNAME_FALLBACK;
        }

        return buildLocalNetwork(1789, hostname);
    }

    public static Environment buildLocalhostNetwork()
    {
        return buildLocalNetwork(1789, "localhost");
    }

    public static Environment buildLocalNetwork(int port, String linterface)
    {
        // A single node with a corresponding Place
        Environment n = new Environment();

        // Execution node (the local sever)
        ExecutionNode n1 = buildExecutionNode(n, "local", linterface, port);
        n.setConsole(n1);

        // Place
        buildPlace(n, "local", n1);

        return n;
    }

    public static RunnerCommand buildShellCommand(Application a, String command, String name, String description, String... prmsandvalues)
    {
        return buildShellCommand(RunnerConstants.SHELL_WINCMD, a, command, name, description, prmsandvalues);
    }

    public static RunnerCommand buildPowerShellCommand(Application a, String command, String name, String description,
            String... prmsandvalues)
    {
        return buildShellCommand(RunnerConstants.SHELL_POWERSHELL, a, command, name, description, prmsandvalues);
    }

    public static RunnerCommand buildShellCommand(String plugin, Application a, String command, String name, String description,
            String... prmsandvalues)
    {
        RunnerCommand sc1 = new RunnerCommand();
        sc1.addPluginParameter("COMMAND", command);
        sc1.setDescription(description);
        sc1.setName(name);
        sc1.setPlugin(plugin);
        a.addActiveElement(sc1);

        for (int i = 0; i < prmsandvalues.length / 2; i++)
        {
            Parameter pa1 = new Parameter();
            pa1.setDescription("param " + i + " of command " + name);
            pa1.setKey(prmsandvalues[i * 2]);
            pa1.setValue(prmsandvalues[i * 2 + 1]);
            a.addParameter(pa1);
            sc1.addParameter(pa1);
        }
        return sc1;
    }

    public static External buildExternal(Application a, String name)
    {
        return buildExternal(a, name, null);
    }

    public static External buildExternal(Application a, String name, String regExp)
    {
        External e = new External();
        e.setApplication(a);
        e.setRegularExpression(regExp);
        e.setName(name);
        a.addActiveElement(e);

        return e;
    }

    public static NextOccurrence buildNextOccurrence(Application a, Calendar ca)
    {
        NextOccurrence no1 = new NextOccurrence();
        no1.setName("End of calendar occurrence");
        no1.setDescription(no1.getName());
        no1.setUpdatedCalendar(ca);
        a.addActiveElement(no1);

        return no1;
    }

    public static State buildState(Chain c1, PlaceGroup pg1, ActiveNodeBase target)
    {
        return buildState(c1, pg1, target, false);
    }

    public static State buildState(Chain c1, PlaceGroup pg1, ActiveNodeBase target, boolean parallel)
    {
        State s1 = new State();
        s1.setChain(c1);
        s1.setRunsOn(pg1);
        s1.setRepresents(target);
        s1.setX(100);
        s1.setY(100);
        s1.setParallel(parallel);
        return s1;
    }

    public static State buildStateAND(Chain c1, PlaceGroup pg1)
    {
        ActiveNodeBase target = c1.getApplication().getActiveElements(And.class).get(0);
        return buildState(c1, pg1, target, true);
    }

    public static Clock buildClock(Application a, String name, String description, ClockRRule... rulesADD)
    {
        Clock ck1 = new Clock();
        ck1.setDescription(description);
        ck1.setName(name);
        for (ClockRRule r : rulesADD)
        {
            ck1.addRRuleADD(r);
        }
        a.addActiveElement(ck1);

        return ck1;
    }

    public static ClockRRule buildRRuleWeekDays(Application a)
    {
        ClockRRule rr1 = new ClockRRule();
        rr1.setName("Monday-Friday days");
        rr1.setDescription("Every day from monday to friday included, every week");
        rr1.setBYDAY("MO,TU,WE,TH,FR,");
        rr1.setBYHOUR("10");
        rr1.setBYMINUTE("00");
        rr1.setPeriod(Recur.WEEKLY);
        a.addRRule(rr1);

        return rr1;
    }

    public static ClockRRule buildRRule10Seconds(Application a)
    {
        ClockRRule rr1 = new ClockRRule();
        rr1.setName("Every 10 second");
        rr1.setDescription(rr1.getName());
        rr1.setBYSECOND("00,10,20,30,40,50");
        rr1.setPeriod(Recur.MINUTELY);
        a.addRRule(rr1);

        return rr1;
    }

    public static ClockRRule buildRRuleSeconds(Application a, int stepInSeconds)
    {
        ClockRRule rr1 = new ClockRRule();
        rr1.setName("Every " + stepInSeconds + " seconds");
        rr1.setDescription(rr1.getName());
        rr1.setPeriod(Recur.SECONDLY);
        rr1.setINTERVAL(stepInSeconds);
        a.addRRule(rr1);

        return rr1;
    }

    public static ClockRRule buildRRuleMinutes(Application a, int stepInMinutes)
    {
        ClockRRule rr1 = new ClockRRule();
        rr1.setName("Every " + stepInMinutes + " minutes");
        rr1.setDescription(rr1.getName());
        rr1.setPeriod(Recur.MINUTELY);
        rr1.setINTERVAL(stepInMinutes);
        a.addRRule(rr1);

        return rr1;
    }

    public static Token buildToken(Application a, String name, String description)
    {
        return buildToken(a, name, description, 1);
    }

    public static Token buildToken(Application a, String name, String description, int count)
    {
        return buildToken(a, name, description, count, false);
    }

    public static Token buildToken(Application a, String name, String description, int count, boolean byPlace)
    {
        Token t = new Token();
        t.setApplication(a);
        t.setByPlace(byPlace);
        t.setCount(count);
        t.setName(name);
        t.setDescription(description);
        a.addToken(t);

        return t;
    }
}
