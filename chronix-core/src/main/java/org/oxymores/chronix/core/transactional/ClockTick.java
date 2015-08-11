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
import org.joda.time.DateTime;

import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.active.Clock;
import org.sql2o.Connection;

public class ClockTick implements Serializable
{
    private static final long serialVersionUID = 4194251899101238989L;
    protected static final int UUID_LENGTH = 36;

    @NotNull
    private long id;

    @NotNull
    private UUID appId, clockId;

    @NotNull
    private DateTime tickTime;

    public Clock getClock(ChronixContext ctx)
    {
        return (Clock) ctx.getApplication(this.appId).getActiveNode(this.clockId);
    }

    public UUID getClockId()
    {
        return clockId;
    }

    public void setClockId(UUID clockId)
    {
        this.clockId = clockId;
    }

    public DateTime getTickTime()
    {
        return tickTime;
    }

    public void setTickTime(DateTime tickTime)
    {
        this.tickTime = tickTime;
    }

    public UUID getAppId()
    {
        return appId;
    }

    public void setAppId(UUID appId)
    {
        this.appId = appId;
    }

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public void insert(Connection conn)
    {
        conn.createQuery("INSERT INTO ClockTick(appID, clockID, tickTime) VALUES(:appId, :clockId, :tickTime)")
                .bind(this).executeUpdate();
    }
}
