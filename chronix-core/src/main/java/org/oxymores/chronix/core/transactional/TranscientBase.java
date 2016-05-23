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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.EventSourceDef;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.network.Place;
import org.sql2o.Connection;

public class TranscientBase implements Serializable
{
    private static final long serialVersionUID = 8976655465578L;
    protected static final int UUID_LENGTH = 36;
    protected static final int DESCR_LENGTH = 100;
    protected static final int PATH_LENGTH = 1024;
    protected static final int LOG_LENGTH = 10000;
    protected static final String DATE_FORMAT = "dd/MM HH:mm:ss";

    @NotNull
    protected UUID id;

    @NotNull
    protected UUID stateID, activeID, placeID, appID;

    protected UUID calendarID, calendarOccurrenceID;

    @NotNull
    protected UUID simulationID;

    protected Boolean outsideChainLaunch = false;

    protected Boolean ignoreCalendarUpdating = false;

    @NotNull
    protected DateTime createdAt;

    protected DateTime virtualTime;

    protected UUID level0Id, level1Id, level2Id, level3Id;

    /** Simple cache for environment variables associated to this item **/
    protected List<EnvironmentValue> envValues;

    public TranscientBase()
    {
        id = UUID.randomUUID();
        createdAt = DateTime.now();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof TranscientBase))
        {
            return false;
        }
        return ((TranscientBase) o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }

    // //////////////////////////////////////////////
    // Calendar & co
    public UUID getCalendarID()
    {
        return calendarID;
    }

    public void setCalendarID(UUID id)
    {
        this.calendarID = id;
    }

    public FunctionalSequence getCalendar(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getCalendar(this.calendarID);
    }

    public void setCalendar(FunctionalSequence c)
    {
        this.calendarID = c.getId();
    }

    public UUID getCalendarOccurrenceID()
    {
        return calendarOccurrenceID;
    }

    public void setCalendarOccurrenceID(UUID calendarOccurrenceID)
    {
        this.calendarOccurrenceID = calendarOccurrenceID;
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // State
    protected void setStateID(UUID stateID)
    {
        this.stateID = stateID;
    }

    public void setState(State state)
    {
        if (state != null)
        {
            this.stateID = state.getId();
            this.setActiveID(state.getEventSourceDefinition().getId());
            this.setApplication(state.getApplication());
        }
        else
        {
            this.stateID = null;
        }
    }

    public State getState(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getState(this.stateID);
    }

    public UUID getStateID()
    {
        return this.stateID;
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Active node
    public UUID getActiveID()
    {
        return activeID;
    }

    protected void setActiveID(UUID activeID)
    {
        this.activeID = activeID;
    }

    public EventSourceDef getActive(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getEventSource(this.activeID);
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Environment
    public UUID getPlaceID()
    {
        return placeID;
    }

    protected void setPlaceID(UUID placeID)
    {
        this.placeID = placeID;
    }

    public void setPlace(Place place)
    {
        if (place != null)
        {
            this.placeID = place.getId();
        }
        else
        {
            this.placeID = null;
        }
    }

    public Place getPlace(ChronixContextMeta ctx)
    {
        return ctx.getEnvironment().getPlace(this.placeID);
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Misc.
    public UUID getAppID()
    {
        return appID;
    }

    public void setAppID(UUID appID)
    {
        this.appID = appID;
    }

    public Application getApplication(ChronixContextMeta ctx)
    {
        return ctx.getApplication(this.appID);
    }

    public void setApplication(Application application)
    {
        if (application != null)
        {
            this.appID = application.getId();
        }
        else
        {
            this.appID = null;
        }
    }

    public DateTime getCreatedAt()
    {
        return createdAt;
    }

    protected void setCreatedAt(DateTime created)
    {
        this.createdAt = created;
    }

    public UUID getId()
    {
        return id;
    }

    protected void setId(UUID id)
    {
        this.id = id;
    }

    public List<EnvironmentValue> getEnvValues(Connection conn)
    {
        if (this.envValues == null)
        {
            this.envValues = conn.createQuery("SELECT * FROM EnvironmentValue WHERE TransientId=:id").addParameter("id", this.id)
                    .executeAndFetch(EnvironmentValue.class);
        }
        return envValues;
    }

    /**
     * Does not persist the value - only stores it in local cache
     * 
     * @param key
     * @param value
     **/
    public void addEnvValueToCache(String key, String value)
    {
        if (this.envValues == null)
        {
            this.envValues = new ArrayList<>();
        }
        EnvironmentValue tmp = new EnvironmentValue(key, value, this);
        this.envValues.add(tmp);
    }

    public Boolean getOutsideChain()
    {
        return outsideChainLaunch;
    }

    public void setOutsideChain(Boolean outsideChain)
    {
        this.outsideChainLaunch = outsideChain;
    }

    public Boolean getIgnoreCalendarUpdating()
    {
        return ignoreCalendarUpdating;
    }

    public void setIgnoreCalendarUpdating(Boolean ignoreCalendarUpdating)
    {
        this.ignoreCalendarUpdating = ignoreCalendarUpdating;
    }

    public UUID getSimulationID()
    {
        return simulationID;
    }

    public void setSimulationID(UUID simulationID)
    {
        this.simulationID = simulationID;
    }

    public UUID getLevel0Id()
    {
        return level0Id;
    }

    public void setLevel0Id(UUID level0Id)
    {
        this.level0Id = level0Id;
    }

    public UUID getLevel1Id()
    {
        return level1Id;
    }

    public void setLevel1Id(UUID level1Id)
    {
        this.level1Id = level1Id;
    }

    public UUID getLevel2Id()
    {
        return level2Id;
    }

    public void setLevel2Id(UUID level2Id)
    {
        this.level2Id = level2Id;
    }

    public UUID getLevel3Id()
    {
        return level3Id;
    }

    public void setLevel3Id(UUID level3Id)
    {
        this.level3Id = level3Id;
    }

    //
    // //////////////////////////////////////////////
    public DateTime getVirtualTime()
    {
        return virtualTime;
    }

    public void setVirtualTime(DateTime virtualTime)
    {
        this.virtualTime = virtualTime;
    }

    public void updateEnvValues(Connection conn)
    {
        if (this.envValues != null && this.envValues.size() > 0)
        {
            conn.createQuery("DELETE FROM EnvironmentValue WHERE transientid = :id").addParameter("id", this.id).executeUpdate();
            for (EnvironmentValue ev : this.envValues)
            {
                ev.insert(conn);
            }
        }
    }
}
