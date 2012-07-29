/**
 * @author Marc-Antoine Gouillart
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

package org.oxymores.chronix.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ConfigurableBase;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.NodeLink;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.ShellCommand;
import org.oxymores.chronix.core.active.ShellParameter;

public class DemoApplication {

	public static Application getNewDemoApplication() {
		Application a = new Application();
		a.setname("Demo");
		a.setDescription("test application auto created");

		// Physical network
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
		ExecutionNode n1 = new ExecutionNode();
		n1.setDns(hostname);
		n1.setOspassword("");
		n1.setqPort(1789);
		n1.setX(100);
		n1.setY(100);
		a.addNode(n1);

		ExecutionNode n2 = new ExecutionNode();
		n2.setDns(hostname);
		n2.setOspassword("");
		n2.setqPort(1400);
		n2.setX(200);
		n2.setY(200);
		a.addNode(n2);

		NodeLink l1 = new NodeLink();
		l1.setMethod(NodeConnectionMethod.TCP);
		l1.setNodeFrom(n1);
		l1.setNodeTo(n2);

		// Logical network
		Place p1 = new Place();
		p1.setDescription("place 1");
		p1.setName("place 1");
		p1.setNode(n1);
		a.addPlace(p1);

		Place p2 = new Place();
		p2.setDescription("place 2");
		p2.setName("place 2");
		p2.setNode(n2);
		a.addPlace(p2);

		PlaceGroup pg1 = new PlaceGroup();
		pg1.setDescription("group all");
		pg1.setName("group all");
		a.addGroup(pg1);
		pg1.addPlace(p1);
		pg1.addPlace(p2);

		PlaceGroup pg2 = new PlaceGroup();
		pg2.setDescription("group 1");
		pg2.setName("group 1");
		a.addGroup(pg2);
		p1.addToGroup(pg2);

		PlaceGroup pg3 = new PlaceGroup();
		pg3.setDescription("group 2");
		pg3.setName("group 2");
		a.addGroup(pg3);
		p2.addToGroup(pg3);

		// //////////////////////////////////////////////////////////////
		// Calendars
		// //////////////////////////////////////////////////////////////

		Calendar cal1 = new Calendar();
		cal1.setName("Week days");
		cal1.setDescription("All days from monday to friday for the whole year");
		cal1.setManualSequence(false);
		a.addCalendar(cal1);

		DateTime d = new DateTime(2029, 12, 31, 0, 0);
		for (int i = 0; i <= 365; i++) {
			d = d.plusDays(1);
			if (d.getDayOfWeek() > 5)
				continue;

			new CalendarDay(d.toString("dd/MM/yyyy"), cal1);
		}

		// //////////////////////////////////////////////////////////////
		// Sources
		// //////////////////////////////////////////////////////////////

		// ////////////////////
		// Shell commands
		ShellCommand sc1 = new ShellCommand();
		sc1.setCommand("echo");
		sc1.setDescription("command 1");
		sc1.setName("command 1");
		a.addActiveElement(sc1);

		ShellCommand sc2 = new ShellCommand();
		sc2.setCommand("echo c2");
		sc2.setDescription("command 2");
		sc2.setName("command 2");
		a.addActiveElement(sc2);

		ShellCommand sc3 = new ShellCommand();
		sc3.setCommand("echo c3");
		sc3.setDescription("command 3");
		sc3.setName("command 3");
		a.addActiveElement(sc3);

		// ////////////////////
		// Chains
		Chain c1 = new Chain();
		c1.setDescription("chain 1");
		c1.setName("chain1");
		a.addActiveElement(c1);

		Chain c2 = new Chain();
		c2.setDescription("chain 2");
		c2.setName("chain2");
		a.addActiveElement(c2);

		Chain c3 = new Chain();
		c3.setDescription("chain 3");
		c3.setName("chain3");
		a.addActiveElement(c3);
		
		Chain c4 = new Chain();
		c4.setDescription("chain 4");
		c4.setName("chain4");
		a.addActiveElement(c4);

		// ////////////////////
		// Calendar next
		NextOccurrence no1 = new NextOccurrence();
		no1.setName("End of calendar occurrence");
		no1.setDescription(no1.getName());
		no1.setUpdatedCalendar(cal1);
		a.addActiveElement(no1);

		// ////////////////////
		// Auto elements retrieval
		ChainStart cs = null;
		for (ConfigurableBase nb : a.getActiveElements().values()) {
			if (nb instanceof ChainStart)
				cs = (ChainStart) nb;
		}
		ChainEnd ce = null;
		for (ConfigurableBase nb : a.getActiveElements().values()) {
			if (nb instanceof ChainEnd)
				ce = (ChainEnd) nb;
		}

		// //////////////////////////////////////////////////////////////
		// Parameters
		// //////////////////////////////////////////////////////////////

		Parameter pa1 = new Parameter();
		pa1.setDescription("param 1");
		pa1.setKey("k");
		pa1.setValue("a");
		a.addParameter(pa1);
		sc1.addParameter(pa1);

		Parameter pa2 = new Parameter();
		pa2.setDescription("param 1");
		pa2.setKey("k");
		pa2.setValue("a");
		a.addParameter(pa2);
		pa2.addElement(sc2);

		Parameter pa3 = new Parameter();
		pa3.setDescription("param 1");
		pa3.setKey("k");
		pa3.setValue("a");
		a.addParameter(pa3);

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

		// Start
		State s1 = new State();
		s1.setChain(c1);
		s1.setRunsOn(pg2);
		s1.setRepresents(cs);
		s1.setX(100);
		s1.setY(100);

		// End
		State s2 = new State();
		s2.setChain(c1);
		s2.setRunsOn(pg2);
		s2.setRepresents(ce);
		s2.setX(300);
		s2.setY(200);
		s2.setCalendar(cal1);

		// Echo c1
		State s3 = new State();
		s3.setChain(c1);
		s3.setRunsOn(pg2);
		s3.setRepresents(sc1);
		s3.setX(300);
		s3.setY(400);

		// Transitions
		s1.connectTo(s3);
		s3.connectTo(s2);

		// ////////////////////
		// Chain 2 : simple S -> T2 -> E

		// Start
		State s21 = new State();
		s21.setChain(c2);
		s21.setRunsOn(pg2);
		s21.setRepresents(cs);
		s21.setX(60);
		s21.setY(60);

		// End
		State s22 = new State();
		s22.setChain(c2);
		s22.setRunsOn(pg2);
		s22.setRepresents(ce);
		s22.setX(300);
		s22.setY(400);

		// Echo c1
		State s23 = new State();
		s23.setChain(c2);
		s23.setRunsOn(pg2);
		s23.setRepresents(sc2);
		s23.setX(200);
		s23.setY(250);

		// Transitions
		s21.connectTo(s23);
		s23.connectTo(s22);

		// ////////////////////
		// Chain 3 : simple S -> T3 -> E

		// Start
		State s31 = new State();
		s31.setChain(c3);
		s31.setRunsOn(pg2);
		s31.setRepresents(cs);
		s31.setX(60);
		s31.setY(60);

		// End
		State s32 = new State();
		s32.setChain(c3);
		s32.setRunsOn(pg2);
		s32.setRepresents(ce);
		s32.setX(300);
		s32.setY(400);

		// Echo c1
		State s33 = new State();
		s33.setChain(c3);
		s33.setRunsOn(pg2);
		s33.setRepresents(sc3);
		s33.setX(200);
		s33.setY(250);

		// Transitions
		s31.connectTo(s33);
		s33.connectTo(s32);

		// ////////////////////
		// Chain 4 : simple S -> END CALENDAR -> E

		// Start
		State s41 = new State();
		s41.setChain(c4);
		s41.setRunsOn(pg2);
		s41.setRepresents(cs);
		s41.setX(60);
		s41.setY(60);

		// End
		State s42 = new State();
		s42.setChain(c4);
		s42.setRunsOn(pg2);
		s42.setRepresents(ce);
		s42.setX(300);
		s42.setY(400);

		// Echo c1
		State s43 = new State();
		s43.setChain(c4);
		s43.setRunsOn(pg2);
		s43.setRepresents(no1);
		s43.setX(200);
		s43.setY(250);

		// Transitions
		s41.connectTo(s43);
		s43.connectTo(s42);

		return a;
	}
}
