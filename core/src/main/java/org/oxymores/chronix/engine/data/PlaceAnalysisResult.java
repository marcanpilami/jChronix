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

package org.oxymores.chronix.engine.data;

import java.util.ArrayList;

import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.transactional.Event;

public class PlaceAnalysisResult
{
    public PlaceAnalysisResult(Place p)
    {
        this.place = p;
    }

    public boolean res = false;
    public Place place;
    public ArrayList<Event> consumedEvents = new ArrayList<Event>();
    public ArrayList<Event> usedEvents = new ArrayList<Event>();

    public void add(PlaceAnalysisResult ear)
    {
        res = res && ear.res;
        if (res)
            consumedEvents.addAll(ear.consumedEvents);
        else
            consumedEvents.clear();

        if (res)
            usedEvents.addAll(ear.usedEvents);
        else
            usedEvents.clear();
    }

    public Place getPlace()
    {
        return place;
    }
}
