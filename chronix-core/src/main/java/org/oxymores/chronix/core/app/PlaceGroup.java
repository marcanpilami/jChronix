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
package org.oxymores.chronix.core.app;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.NamedApplicationObject;
import org.oxymores.chronix.core.network.Place;

public class PlaceGroup extends NamedApplicationObject
{
    private static final long serialVersionUID = 4569641718657486177L;

    transient protected List<Place> places = new ArrayList<>();

    public PlaceGroup()
    {
        super();
    }

    private Object readResolve()
    {
        if (places == null)
        {
            places = new ArrayList<>();
        }
        return this;
    }

    public List<Place> getPlaces()
    {
        return new ArrayList<>(places);
    }

    public void addPlace(Place p)
    {
        p.addGroupMembership(this);
        this.places.add(p);
    }

    public void map_places(Environment n)
    {
        if (this.places == null)
        {
            // Happens on deserialization
            this.places = new ArrayList<>();
        }

        for (Place p : n.getPlaces().values())
        {
            for (UUID groupId : p.getMemberOfIds())
            {
                if (groupId.equals(this.getId()))
                {
                    this.places.add(p);
                    break;
                }
            }
        }
    }
}
