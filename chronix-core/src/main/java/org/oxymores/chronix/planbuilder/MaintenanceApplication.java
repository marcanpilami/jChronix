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

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.PlaceGroup;
import org.oxymores.chronix.core.State;

public final class MaintenanceApplication
{
    private MaintenanceApplication()
    {

    }

    public static Application getNewApplication(Place localPlace)
    {
        Application a = PlanBuilder.buildApplication("Chronix Maintenance", "All the jobs needed to keep the local scheduler node running properly");

        PlaceGroup pg = PlanBuilder.buildPlaceGroup(a, "local node", "local node", localPlace);
        Chain c1 = PlanBuilder.buildChain(a, "Maintenance plan", "all the default maintenance jobs", pg);

        State s1 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo history_purge", "History purge", "Will purge the history table", "-d", "10"));
        State s2 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo trace_purge", "Trace purge", "Will purge the performance trace table", "-d", "2"));
        State s3 = PlanBuilder.buildState(c1, pg, PlanBuilder.buildShellCommand(a, "echo compile_purge", "Compile purge", "Will aggregate the performance trace table data"));

        c1.getStartState().connectTo(s1);
        s1.connectTo(s2);
        s1.connectTo(s3);

        return a;
    }
}
