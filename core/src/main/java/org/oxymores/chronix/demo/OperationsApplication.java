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
