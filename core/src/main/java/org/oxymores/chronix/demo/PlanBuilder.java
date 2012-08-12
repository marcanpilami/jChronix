package org.oxymores.chronix.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.fortuna.ical4j.model.Recur;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ConfigurableBase;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.And;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;
import org.oxymores.chronix.core.active.Clock;
import org.oxymores.chronix.core.active.ClockRRule;
import org.oxymores.chronix.core.active.NextOccurrence;
import org.oxymores.chronix.core.active.ShellCommand;

public class PlanBuilder {

	public static Application buildApplication(String name, String description) {
		Application a = new Application();
		a.setname(name);
		a.setDescription("test application auto created");

		return a;
	}

	public static Chain buildChain(Application a, String name, String description, PlaceGroup targets) {
		Chain c1 = new Chain();
		c1.setDescription(description);
		c1.setName(name);
		a.addActiveElement(c1);

		// Start & end retrieval
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

	public static ExecutionNode buildExecutionNode(Application a, int port) {
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
		return buildExecutionNode(a, hostname, port);
	}

	public static ExecutionNode buildExecutionNode(Application a, String dns, int port) {
		ExecutionNode n1 = new ExecutionNode();
		n1.setDns(dns);
		n1.setOspassword("");
		n1.setqPort(port);
		n1.setX(100);
		n1.setY(100);
		a.addNode(n1);

		return n1;
	}

	public static Place buildPlace(Application a, String name, String description, ExecutionNode en) {
		Place p1 = new Place();
		p1.setDescription(description);
		p1.setName(name);
		p1.setNode(en);
		a.addPlace(p1);

		return p1;
	}

	public static PlaceGroup buildPlaceGroup(Application a, String name, String description, Place... places) {
		PlaceGroup pg1 = new PlaceGroup();
		pg1.setDescription(description);
		pg1.setName(name);
		a.addGroup(pg1);

		for (Place p : places)
			pg1.addPlace(p);

		return pg1;
	}

	public static PlaceGroup buildDefaultLocalNetwork(Application a) {
		// Execution node (the local sever)
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getCanonicalHostName();
		} catch (UnknownHostException e) {
			hostname = "localhost";
		}
		ExecutionNode n1 = buildExecutionNode(a, hostname, 1789);

		// Place
		Place p1 = buildPlace(a, hostname, "the local server", n1);

		// Group with only this place
		PlaceGroup pg1 = buildPlaceGroup(a, hostname, "the local server", p1);

		return pg1;
	}

	public static ShellCommand buildShellCommand(Application a, String command, String name, String description, String... prmsandvalues) {
		ShellCommand sc1 = new ShellCommand();
		sc1.setCommand(command);
		sc1.setDescription(description);
		sc1.setName(name);
		a.addActiveElement(sc1);

		for (int i = 0; i < prmsandvalues.length / 2; i++) {
			Parameter pa1 = new Parameter();
			pa1.setDescription("param " + i + " of command " + name);
			pa1.setKey(prmsandvalues[i * 2]);
			pa1.setValue(prmsandvalues[i * 2 + 1]);
			a.addParameter(pa1);
			sc1.addParameter(pa1);
		}
		return sc1;
	}

	public static NextOccurrence buildNextOccurrence(Application a, Calendar ca) {
		NextOccurrence no1 = new NextOccurrence();
		no1.setName("End of calendar occurrence");
		no1.setDescription(no1.getName());
		no1.setUpdatedCalendar(ca);
		a.addActiveElement(no1);

		return no1;
	}

	public static State buildState(Chain c1, PlaceGroup pg1, ActiveNodeBase target) {
		return buildState(c1, pg1, target, false);
	}

	public static State buildState(Chain c1, PlaceGroup pg1, ActiveNodeBase target, boolean parallel) {
		State s1 = new State();
		s1.setChain(c1);
		s1.setRunsOn(pg1);
		s1.setRepresents(target);
		s1.setX(100);
		s1.setY(100);
		s1.setParallel(parallel);
		return s1;
	}

	public static State buildStateAND(Chain c1, PlaceGroup pg1) {
		ActiveNodeBase target = c1.getApplication().getActiveElements(And.class).get(0);
		return buildState(c1, pg1, target, true);
	}

	public static Clock buildClock(Application a, String name, String description, ClockRRule... rulesADD) {
		Clock ck1 = new Clock();
		ck1.setDescription(description);
		ck1.setName(name);
		for (ClockRRule r : rulesADD)
			ck1.addRRuleADD(r);
		a.addActiveElement(ck1);

		return ck1;
	}

	public static ClockRRule buildRRuleWeekDays(Application a) {
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

	public static ClockRRule buildRRule10Seconds(Application a) {
		ClockRRule rr1 = new ClockRRule();
		rr1.setName("Every 10 second");
		rr1.setDescription("Every 10 second");
		rr1.setBYSECOND("00,10,20,30,40,50");
		rr1.setPeriod(Recur.MINUTELY);
		a.addRRule(rr1);

		return rr1;
	}
}
