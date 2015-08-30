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

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.active.ShellParameter;

public final class DemoApplication
{
    private DemoApplication()
    {

    }

    public static Application getNewDemoApplication()
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
        return getNewDemoApplication(hostname, 1789);
    }

    // TODO: cut appli and network
    public static Application getNewDemoApplication(String brokerInterface, int brokerPort)
    {
        Application a = PlanBuilder.buildApplication("Demo", "test application auto created");

        // Physical network
        Environment n = new Environment();
        ExecutionNode n1 = PlanBuilder.buildExecutionNode(n, "first", brokerInterface, brokerPort, 100, 100);
        n1.setConsole(true);
        ExecutionNode n2 = PlanBuilder.buildExecutionNode(n, "second", brokerInterface, 1400, 200, 200);
        n1.connectTo(n2, NodeConnectionMethod.TCP);

        // Logical network
        Place p1 = PlanBuilder.buildPlace(n, "place 1", n1);
        Place p2 = PlanBuilder.buildPlace(n, "place 2", n1);
        Place p3 = PlanBuilder.buildPlace(n, "place 3", n1);

        PlanBuilder.buildPlaceGroup(a, "group all", "test group all", p1, p2, p3);
        PlaceGroup pg2 = PlanBuilder.buildPlaceGroup(a, "group 1", "test group 1", p1);
        PlanBuilder.buildPlaceGroup(a, "group 2", "test group 2", p1);
        PlanBuilder.buildPlaceGroup(a, "group 3", "test group 3", p1);

        // //////////////////////////////////////////////////////////////
        // Calendars
        // //////////////////////////////////////////////////////////////
        Calendar cal1 = CalendarBuilder.buildWorkDayCalendar(a, 2029);

        // //////////////////////////////////////////////////////////////
        // Sources
        // //////////////////////////////////////////////////////////////
        // ////////////////////
        // Shell commands
        ShellCommand sc1 = PlanBuilder.buildShellCommand(a, "echo", "command 1", "test command 1");
        ShellCommand sc2 = PlanBuilder.buildShellCommand(a, "echo c2", "command 2", "test command 2");
        ShellCommand sc3 = PlanBuilder.buildShellCommand(a, "echo c3", "command 3", "test command 3");

        // ////////////////////
        // Chains
        Chain c1 = PlanBuilder.buildChain(a, "chain 1", "test chain 1", pg2);
        Chain c2 = PlanBuilder.buildChain(a, "chain 2", "test chain 2", pg2);
        Chain c3 = PlanBuilder.buildChain(a, "chain 3", "test chain 3", pg2);
        Chain c4 = PlanBuilder.buildChain(a, "chain 4", "test chain 4", pg2);

        // ////////////////////
        // Calendar next
        NextOccurrence no1 = PlanBuilder.buildNextOccurrence(a, cal1);

        // ////////////////////
        // Clocks
        ClockRRule rr1 = PlanBuilder.buildRRuleWeekDays(a);
        Clock clock1 = PlanBuilder.buildClock(a, "every workday", "test clock", rr1);

        // //////////////////////////////////////////////////////////////
        // Parameters
        // //////////////////////////////////////////////////////////////
        sc1.addParameter("k", "a", "param 1 for command 1");
        sc2.addParameter("k", "a", "param 1 for command 2");

        ShellParameter pa4 = new ShellParameter();
        pa4.setDescription("test shell param");
        pa4.setKey("");
        pa4.setValue("echo dynamic");
        a.addParameter(pa4);
        sc1.addParameter(pa4);

        // //////////////////////////////////////////////////////////////
        // State/Transition
        // //////////////////////////////////////////////////////////////
        // ////////////////////
        // Chain 1 : simple S -> T1 -> E
        // Echo c1
        State s1 = PlanBuilder.buildState(c1, pg2, sc1);
        s1.setX(300);
        s1.setY(400);

        // Transitions
        c1.getStartState().connectTo(s1);
        s1.connectTo(c1.getEndState());

        // ////////////////////
        // Chain 2 : simple S -> T2 -> E with calendar
        // Echo c1
        State s21 = PlanBuilder.buildState(c2, pg2, sc2);
        s21.setX(200);
        s21.setY(250);
        s21.setCalendar(cal1);

        // Transitions
        c2.getStartState().connectTo(s21);
        s21.connectTo(c2.getEndState());

        // ////////////////////
        // Chain 3 : simple S -> T3 -> E
        // Echo c1
        State s31 = PlanBuilder.buildState(c3, pg2, sc3);
        s31.setChain(c3);
        s31.setRunsOn(pg2);
        s31.setRepresents(sc3);
        s31.setX(200);
        s31.setY(250);

        // Transitions
        c3.getStartState().connectTo(s31);
        s31.connectTo(c3.getEndState());

        // ////////////////////
        // Chain 4 : simple S -> END CALENDAR -> E
        // End calendar state
        State s41 = PlanBuilder.buildState(c4, pg2, no1);
        s41.setX(200);
        s41.setY(250);

        // Transitions
        c4.getStartState().connectTo(s41);
        s41.connectTo(c4.getEndState());

        // //////////////////////////////////////////////////////////////
        // Master plan
        // //////////////////////////////////////////////////////////////
        Chain plan1 = PlanBuilder.buildPlan(a, "demo plan", "default plan containing all chains");
        State s_plan1_1 = PlanBuilder.buildState(plan1, pg2, clock1);
        State s_plan1_2 = PlanBuilder.buildState(plan1, pg2, c1);
        State s_plan1_3 = PlanBuilder.buildState(plan1, pg2, c2);
        State s_plan1_4 = PlanBuilder.buildState(plan1, pg2, c3);
        State s_plan1_5 = PlanBuilder.buildState(plan1, pg2, c4);
        State s_plan1_6 = PlanBuilder.buildStateAND(plan1, pg2);

        s_plan1_1.connectTo(s_plan1_2);
        s_plan1_1.connectTo(s_plan1_3);
        s_plan1_1.connectTo(s_plan1_4);

        s_plan1_2.connectTo(s_plan1_6);
        s_plan1_3.connectTo(s_plan1_6);
        s_plan1_4.connectTo(s_plan1_6);

        s_plan1_6.connectTo(s_plan1_5);

        return a;
    }
}
