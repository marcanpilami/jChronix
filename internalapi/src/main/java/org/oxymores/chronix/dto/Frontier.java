package org.oxymores.chronix.dto;

import java.util.ArrayList;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ConfigurableBase;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.NodeConnectionMethod;
import org.oxymores.chronix.core.NodeLink;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.active.ShellCommand;

public class Frontier {

	public static DTOApplication getApplication(Application a) {
		DTOApplication res = new DTOApplication();

		res.id = a.getId().toString();
		res.name = a.getName();
		res.description = a.getDescription();

		res.chains = new ArrayList<DTOChain>();
		res.shells = new ArrayList<DTOShellCommand>();
		res.places = new ArrayList<DTOPlace>();
		res.groups = new ArrayList<DTOPlaceGroup>();
		res.parameters = new ArrayList<DTOParameter>();
		
		res.nodes = getNetwork(a);

		for (ConfigurableBase o : a.getActiveElements().values()) {
			if (o instanceof Chain) {
				Chain c = (Chain) o;
				res.chains.add(getChain(c));
			}

			if (o instanceof ShellCommand) {
				ShellCommand s = (ShellCommand) o;
				DTOShellCommand d = new DTOShellCommand();
				d.id = s.getId().toString();
				d.command = s.getCommand();
				d.name = s.getName();
				d.description = s.getDescription();
				res.shells.add(d);
			}
		}

		return res;
	}

	public static DTOChain getChain(Chain c) {
		DTOChain res = new DTOChain();
		res.id = c.getId().toString();
		res.name = c.getName();
		res.description = c.getDescription();
		res.states = new ArrayList<DTOState>();
		res.transitions = new ArrayList<DTOTransition>();

		for (State s : c.getStates()) {
			DTOState t = new DTOState();
			t.id = s.getId().toString();
			t.x = s.getX();
			t.y = s.getY();
			t.label = s.getRepresents().getName();
			t.representsId = s.getRepresents().getId().toString();
			try {
				t.runsOnName = s.getRunsOn().getName();
				t.runsOnId = s.getRunsOn().getId().toString();
			} catch (Exception e) {
			}
			res.states.add(t);
		}

		for (Transition o : c.getTransitions()) {
			DTOTransition d = new DTOTransition();
			d.id = o.getId().toString();
			d.from = o.getStateFrom().getId().toString();
			d.to = o.getStateTo().getId().toString();
			d.guard1 = o.getGuard1();
			d.guard2 = o.getGuard2();
			d.guard3 = o.getGuard3();
			d.guard4 = (o.getGuard4() == null ? "" : o.getGuard4().toString());

			res.transitions.add(d);
		}

		return res;
	}

	public static ArrayList<DTOExecutionNode> getNetwork(Application a) {
		ArrayList<DTOExecutionNode> res = new ArrayList<DTOExecutionNode>();
		for (ExecutionNode en : a.getNodes().values())
			res.add(getExecutionNode(en));

		return res;
	}

	public static DTOExecutionNode getExecutionNode(ExecutionNode en) {
		DTOExecutionNode res = new DTOExecutionNode();
		res.id = en.getId().toString();
		res.certFilePath = en.getSshKeyFilePath();
		res.dns = en.getDns();
		res.isConsole = en.isConsole();
		res.jmxPort = en.getJmxPort();
		res.ospassword = en.getOspassword();
		res.osusername = en.getOsusername();
		res.qPort = en.getqPort();
		res.remoteExecPort = en.getRemoteExecPort();
		res.wsPort = en.getWsPort();
		res.x = en.getX();
		res.y = en.getY();

		res.fromRCTRL = new ArrayList<String>();
		res.fromTCP = new ArrayList<String>();
		res.toRCTRL = new ArrayList<String>();
		res.toTCP = new ArrayList<String>();

		for (NodeLink nl : en.getCanSendTo()) {
			if (nl.getMethod() == NodeConnectionMethod.RCTRL)
				res.toRCTRL.add(nl.getNodeTo().getId().toString());
			if (nl.getMethod() == NodeConnectionMethod.TCP)
				res.toTCP.add(nl.getNodeTo().getId().toString());
		}
		for (NodeLink nl : en.getCanReceiveFrom()) {
			if (nl.getMethod() == NodeConnectionMethod.RCTRL)
				res.fromRCTRL.add(nl.getNodeFrom().getId().toString());
			if (nl.getMethod() == NodeConnectionMethod.TCP)
				res.fromTCP.add(nl.getNodeFrom().getId().toString());
		}

		return res;
	}
}
