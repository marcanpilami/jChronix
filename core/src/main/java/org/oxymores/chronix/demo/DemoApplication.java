package org.oxymores.chronix.demo;

import org.oxymores.chronix.core.*;
import org.oxymores.chronix.core.active.*;

public class DemoApplication {

	public static Application getNewDemoApplication() {
		Application a = new Application();
		a.setname("Demo");

		// Physical network
		ExecutionNode n1 = new ExecutionNode();
		n1.setDns("localhost");
		n1.setOspassword("");
		n1.setqPort(1789);
		a.addNode(n1);

		ExecutionNode n2 = new ExecutionNode();
		n2.setDns("localhost");
		n2.setOspassword("");
		n2.setqPort(1400);
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
		// Sources
		// //////////////////////////////////////////////////////////////

		// ////////////////////
		// Shell commands
		ShellCommand sc1 = new ShellCommand();
		sc1.setCommand("echo c1");
		sc1.setDescription("command 1");
		sc1.setName("command 1");
		a.addElement(sc1);

		ShellCommand sc2 = new ShellCommand();
		sc2.setCommand("echo c2");
		sc2.setDescription("command 2");
		sc2.setName("command 2");
		a.addElement(sc2);

		ShellCommand sc3 = new ShellCommand();
		sc3.setCommand("echo c3");
		sc3.setDescription("command 3");
		sc3.setName("command 3");
		a.addElement(sc3);

		// ////////////////////
		// Chains
		Chain c1 = new Chain();
		c1.setDescription("chain 1");
		c1.setName("chain1");
		a.addElement(c1);

		Chain c2 = new Chain();
		c2.setDescription("chain 2");
		c2.setName("chain2");
		a.addElement(c2);

		// ////////////////////
		// Auto elements retrieval
		ChainStart cs = null;
		for (ConfigNodeBase nb : a.getElements()) {
			if (nb instanceof ChainStart)
				cs = (ChainStart) nb;
		}
		ChainEnd ce = null;
		for (ConfigNodeBase nb : a.getElements()) {
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
		a.addElement(s1);

		// End
		State s2 = new State();
		s2.setChain(c1);
		s2.setRunsOn(pg2);
		s2.setRepresents(ce);
		a.addElement(s2);

		// Echo c1
		State s3 = new State();
		s3.setChain(c1);
		s3.setRunsOn(pg2);
		s3.setRepresents(sc1);
		a.addElement(s3);

		// Transitions
		s1.connectTo(s3);
		s3.connectTo(s2);

		return a;
	}
}
