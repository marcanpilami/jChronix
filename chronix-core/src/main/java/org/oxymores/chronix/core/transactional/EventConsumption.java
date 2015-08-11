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
package org.oxymores.chronix.core.transactional;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.sql2o.Connection;

public class EventConsumption
{
    private static final long serialVersionUID = 4960077419503476652L;

    @NotNull
    private UUID eventID;

    @NotNull
    private UUID stateID;

    @NotNull
    private UUID placeID;

    @NotNull
    private UUID appID;

    public UUID getEventID()
    {
        return eventID;
    }

    public void setEventID(UUID eventID)
    {
        this.eventID = eventID;
    }

    public UUID getStateID()
    {
        return stateID;
    }

    public void setStateID(UUID stateID)
    {
        this.stateID = stateID;
    }

    public UUID getPlaceID()
    {
        return placeID;
    }

    public void setPlaceID(UUID placeID)
    {
        this.placeID = placeID;
    }

    public UUID getAppID()
    {
        return appID;
    }

    public void setAppID(UUID appID)
    {
        this.appID = appID;
    }

    public void insert(Connection conn)
    {
        conn.createQuery("INSERT INTO EventConsumption(appID, eventID, placeID, stateID) VALUES(:appID, :eventID, :placeID, :stateID)").bind(this).executeUpdate();
    }
}
