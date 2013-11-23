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
import java.util.Date;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;

@Entity
public class TranscientBase implements Serializable
{
    private static final long serialVersionUID = 8976655465578L;
    // private static Logger log = Logger.getLogger(TranscientBase.class);

    @Id
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String id;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String stateID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String activeID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String placeID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String appID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String calendarOccurrenceID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String calendarID;
    @Column(columnDefinition = "CHAR(36)", length = 36)
    protected String simulationID;
    protected Boolean outsideChainLaunch = false;
    protected Boolean ignoreCalendarUpdating = false;

    protected Date createdAt;
    protected Date virtualTime;

    @OneToMany(fetch = FetchType.EAGER, targetEntity = EnvironmentValue.class, cascade = { CascadeType.ALL, CascadeType.REMOVE })
    protected ArrayList<EnvironmentValue> envParams;

    public TranscientBase()
    {
        id = UUID.randomUUID().toString();
        createdAt = new Date();
        envParams = new ArrayList<EnvironmentValue>();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof TranscientBase))
            return false;
        return ((TranscientBase) o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        return UUID.fromString(this.id).hashCode();
    }

    // //////////////////////////////////////////////
    // Calendar & co
    public String getCalendarID()
    {
        return calendarID;
    }

    public void setCalendarID(String id)
    {
        this.calendarID = id;
    }

    public Calendar getCalendar(ChronixContext ctx)
    {
        return this.getApplication(ctx).getCalendar(UUID.fromString(this.calendarID));
    }

    public void setCalendar(Calendar c)
    {
        if (c == null)
        {
            this.calendarID = null;
        }
        else
        {
            this.calendarID = c.getId().toString();
        }
    }

    public String getCalendarOccurrenceID()
    {
        return calendarOccurrenceID;
    }

    public void setCalendarOccurrenceID(String calendarOccurrenceID)
    {
        this.calendarOccurrenceID = calendarOccurrenceID;
    }

    //
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // State
    protected void setStateID(String stateID)
    {
        this.stateID = stateID;
    }

    public void setState(State state)
    {
        if (state != null)
        {
            this.stateID = state.getId().toString();
            this.setActive(state.getRepresents());
            this.setApplication(state.getApplication());
        }
        else
        {
            this.stateID = null;
        }
    }

    public State getState(ChronixContext ctx)
    {
        return this.getApplication(ctx).getState(UUID.fromString(this.stateID));
    }

    public String getStateID()
    {
        return this.stateID;
    }

    public UUID getStateIDU()
    {
        return UUID.fromString(this.stateID);
    }

    //
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // Active node
    public String getActiveID()
    {
        return activeID;
    }

    protected void setActiveID(String activeID)
    {
        this.activeID = activeID;
    }

    private void setActive(ActiveNodeBase active)
    {
        this.activeID = active.getId().toString();
    }

    public ActiveNodeBase getActive(ChronixContext ctx)
    {
        return this.getApplication(ctx).getActiveNode(UUID.fromString(this.activeID));
    }

    //
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // Network
    public String getPlaceID()
    {
        return placeID;
    }

    protected void setPlaceID(String placeID)
    {
        this.placeID = placeID;
    }

    public void setPlace(Place place)
    {
        if (place != null)
        {
            this.placeID = place.getId().toString();
            this.appID = place.getApplication().getId().toString();
        }
        else
            this.placeID = null;
    }

    public Place getPlace(ChronixContext ctx)
    {
        return this.getApplication(ctx).getPlace(UUID.fromString(this.placeID));
    }

    //
    // //////////////////////////////////////////////

    // //////////////////////////////////////////////
    // Misc.
    public String getAppID()
    {
        return appID;
    }

    protected void setAppID(String appID)
    {
        this.appID = appID;
    }

    public Application getApplication(ChronixContext ctx)
    {
        return ctx.getApplication(this.appID);
    }

    public void setApplication(Application application)
    {
        if (application != null)
            this.appID = application.getId().toString();
        else
            this.appID = null;
    }

    public Date getCreatedAt()
    {
        return createdAt;
    }

    @SuppressWarnings("unused")
    private void setCreatedAt(Date created)
    {
        this.createdAt = created;
    }

    public String getId()
    {
        return id;
    }

    public UUID getIdU()
    {
        return UUID.fromString(this.id);
    }

    @SuppressWarnings("unused")
    private void setId(String id)
    {
        this.id = id;
    }

    public ArrayList<EnvironmentValue> getEnvParams()
    {
        return envParams;
    }

    protected void setEnvParams(ArrayList<EnvironmentValue> values)
    {
        this.envParams = values;
    }

    public void addValue(String key, String value)
    {
        this.envParams.add(new EnvironmentValue(key, value));
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

    public String getSimulationID()
    {
        return simulationID;
    }

    public void setSimulationID(String simulationID)
    {
        this.simulationID = simulationID;
    }

    //
    // //////////////////////////////////////////////

    public Date getVirtualTime()
    {
        return virtualTime;
    }

    public void setVirtualTime(Date virtualTime)
    {
        this.virtualTime = virtualTime;
    }
}
