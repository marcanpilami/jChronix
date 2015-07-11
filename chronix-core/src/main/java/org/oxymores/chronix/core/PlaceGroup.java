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

package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class PlaceGroup extends NamedApplicationObject
{
    private static final long serialVersionUID = 4569641718657486177L;

    @NotNull
    @Size(min = 0, max = 255)
    protected List<UUID> places_id = new ArrayList<>();

    transient protected List<Place> places = new ArrayList<>();

    public PlaceGroup()
    {
        super();
    }

    public List<UUID> getPlacesId()
    {
        return this.places_id;
    }

    public List<Place> getPlaces()
    {
        return places;
    }

    public void addPlace(Place p)
    {
        if (!places.contains(p))
        {
            places.add(p);
            places_id.add(p.getId());
        }
    }

    void map_places(Network n)
    {
        if (this.places == null)
        {
            // Happens on deserialization
            this.places = new ArrayList<>();
        }

        for (Place e : n.getPlaces().values())
        {
            for (UUID i : this.places_id)
            {
                if (i.equals(e.getId()))
                {
                    this.places.add(e);
                    break;
                }
            }
        }
    }
}
