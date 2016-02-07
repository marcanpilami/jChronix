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
import java.util.UUID;
import javax.validation.constraints.NotNull;
import org.oxymores.chronix.core.Calendar;

import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.context.Application2;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.sql2o.Connection;

public class CalendarPointer implements Serializable
{
    private static final long serialVersionUID = 6905957323594389673L;

    @NotNull
    protected UUID id, stateID, placeID, appID, calendarID;

    // Updated at end of run
    private UUID lastEndedOkOccurrenceId;

    // Updated before run
    private UUID lastStartedOccurrenceId;

    // Updated after run
    private UUID lastEndedOccurrenceId;
    private UUID nextRunOccurrenceId;

    private Boolean latestFailed = false;

    private Boolean running = false;

    public CalendarPointer()
    {
        this.id = UUID.randomUUID();
    }

    // //////////////////////////////////////////////
    // Helper fields
    public Boolean getRunning()
    {
        return running;
    }

    public void setRunning(Boolean running)
    {
        this.running = running;
    }

    public Boolean getLatestFailed()
    {
        return latestFailed;
    }

    public void setLatestFailed(Boolean latestFailed)
    {
        this.latestFailed = latestFailed;
    }

    // Only for transient listener!
    public void setId(UUID id)
    {
        this.id = id;
    }

    public UUID getId()
    {
        return this.id;
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Last day it ended correctly
    public UUID getLastEndedOkOccurrenceId()
    {
        return lastEndedOkOccurrenceId;
    }

    public UUID getLastEndedOkOccurrenceUuid()
    {
        return lastEndedOkOccurrenceId;
    }

    public void setLastEndedOkOccurrenceId(UUID dayId)
    {
        this.lastEndedOkOccurrenceId = dayId;
    }

    public CalendarDay getLastEndedOkOccurrenceCd(ChronixContextMeta ctx)
    {
        return this.getCalendar(ctx).getDay(this.lastEndedOkOccurrenceId);
    }

    public void setLastEndedOkOccurrenceCd(CalendarDay day)
    {
        this.lastEndedOkOccurrenceId = day.getId();
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Last day it was started
    public UUID getLastStartedOccurrenceId()
    {
        return lastStartedOccurrenceId;
    }

    public void setLastStartedOccurrenceId(UUID dayId)
    {
        this.lastStartedOccurrenceId = dayId;
    }

    public CalendarDay getLastStartedOccurrenceCd(ChronixContextMeta ctx)
    {
        return this.getCalendar(ctx).getDay(this.lastStartedOccurrenceId);
    }

    public void setLastStartedOccurrenceCd(CalendarDay day)
    {
        this.lastStartedOccurrenceId = day.getId();
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Last day it finished (possibly incorrectly)
    public UUID getLastEndedOccurrenceId()
    {
        return lastEndedOccurrenceId;
    }

    public UUID getLastEndedOccurrenceUuid()
    {
        return lastEndedOccurrenceId;
    }

    public void setLastEndedOccurrenceId(UUID dayId)
    {
        this.lastEndedOccurrenceId = dayId;
    }

    public CalendarDay getLastEndedOccurrenceCd(ChronixContextMeta ctx)
    {
        return this.getCalendar(ctx).getDay(this.lastEndedOccurrenceId);
    }

    public void setLastEndedOccurrenceCd(CalendarDay day)
    {
        this.lastEndedOccurrenceId = day.getId();
    }

    //
    // //////////////////////////////////////////////
    // //////////////////////////////////////////////
    // Next time it will run, it will be...
    public UUID getNextRunOccurrenceId()
    {
        return nextRunOccurrenceId;
    }

    public void setNextRunOccurrenceId(UUID dayId)
    {
        this.nextRunOccurrenceId = dayId;
    }

    public CalendarDay getNextRunOccurrenceCd(ChronixContextMeta ctx)
    {
        return this.getCalendar(ctx).getDay(this.nextRunOccurrenceId);
    }

    public void setNextRunOccurrenceCd(CalendarDay day)
    {
        this.nextRunOccurrenceId = day.getId();
    }

    public Calendar getCalendar(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getCalendar(this.calendarID);
    }

    public UUID getCalendarID()
    {
        return this.calendarID;
    }

    public Application2 getApplication(ChronixContextMeta ctx)
    {
        return ctx.getApplication(this.appID);
    }

    public void setApplication(Application2 a)
    {
        this.appID = a == null ? null : a.getId();
    }

    public void setCalendar(Calendar a)
    {
        this.calendarID = a == null ? null : a.getId();
    }

    public void setPlace(Place p)
    {
        this.placeID = p == null ? null : p.getId();
    }

    public void setState(State s)
    {
        this.stateID = s == null ? null : s.getId();
    }

    public UUID getPlaceID()
    {
        return this.placeID;
    }

    public UUID getStateID()
    {
        return this.stateID;
    }

    public State getState(ChronixContextMeta ctx)
    {
        return this.getApplication(ctx).getState(this.stateID);
    }
    //
    // //////////////////////////////////////////////

    public void insertOrUpdate(Connection conn)
    {
        int i = conn
                .createQuery("UPDATE CalendarPointer SET lastEndedOkOccurrenceId=:lastEndedOkOccurrenceId, "
                        + "lastStartedOccurrenceId=:lastStartedOccurrenceId, nextRunOccurrenceId=:nextRunOccurrenceId, "
                        + "lastEndedOccurrenceId=:lastEndedOccurrenceId, latestFailed=:latestFailed, running=:running " + "WHERE id=:id")
                .bind(this).executeUpdate().getResult();
        if (i == 0)
        {
            conn.createQuery("INSERT INTO CalendarPointer(ID, STATEID, PLACEID, APPID, CALENDARID, lastEndedOkOccurrenceId, "
                    + "lastStartedOccurrenceId, lastEndedOccurrenceId, latestFailed, running, nextRunOccurrenceId) VALUES ("
                    + ":id, :stateID, :placeID, :appID, :calendarID, :lastEndedOkOccurrenceId, :lastStartedOccurrenceId, :lastEndedOccurrenceId,"
                    + ":latestFailed, :running, :nextRunOccurrenceId)").bind(this).executeUpdate();
        }
    }
}
