package org.oxymores.chronix.demo;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.PlaceGroup;

public class OperationsApplication
{

	public static Application getNewApplication(String brokerInterface, int port)
	{
		Application a = PlanBuilder.buildApplication("Operations",
				"This application exists to group all the little 'on demand' jobs that operators often get");

		PlaceGroup pg = PlanBuilder.buildDefaultLocalNetwork(a, port, brokerInterface);
		PlanBuilder.buildChain(a, "All ops", "create as many jobs as you want here", pg);

		return a;
	}
}
