package org.oxymores.chronix.demo;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;

public class MaintenanceApplication {
	public static Application getNewApplication() {
		Application a = PlanBuilder
				.buildApplication(
						"Chronix Maintenance",
						"All the jobs needed to keep the local scheduler node running properly");

		PlaceGroup pg = PlanBuilder.buildDefaultLocalNetwork(a);
		Chain c1 = PlanBuilder.buildChain(a, "Maintenance plan", "all the default maintenance jobs", pg);
		
		State s1 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo purge", "History purge", "Will purge the history table", "-d", "10"));
		State s2 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo purge", "Trace purge", "Will purge the performance trace table", "-d", "2"));
		State s3 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo aggre", "Compile purge", "Will aggregate the performance trace table data"));
		
		c1.getStartState().connectTo(s1);
		s1.connectTo(s2);
		s1.connectTo(s3);
		
		return a;
	}
}
