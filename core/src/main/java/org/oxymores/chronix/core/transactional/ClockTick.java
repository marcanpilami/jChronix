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
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.active.Clock;

@Entity
public class ClockTick implements Serializable
{
    private static final long serialVersionUID = 4194251899101238989L;
    protected static final int UUID_LENGTH = 36;

    @Column(length = UUID_LENGTH, nullable = false)
    private String appId;

    @Column(length = UUID_LENGTH, nullable = false)
    private String clockId;

    @Column(nullable = false)
    private Date tickTime;

    public Clock getClock(ChronixContext ctx)
    {
        return (Clock) ctx.getApplication(this.appId).getActiveNode(UUID.fromString(this.clockId));
    }

    public String getClockId()
    {
        return clockId;
    }

    public void setClockId(String clockId)
    {
        this.clockId = clockId;
    }

    public Date getTickTime()
    {
        return tickTime;
    }

    public void setTickTime(Date tickTime)
    {
        this.tickTime = tickTime;
    }

    public String getAppId()
    {
        return appId;
    }

    public void setAppId(String appId)
    {
        this.appId = appId;
    }
}
