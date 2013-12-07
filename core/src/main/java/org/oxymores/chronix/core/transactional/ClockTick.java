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

import javax.persistence.Entity;

@Entity
public class ClockTick implements Serializable
{
    private static final long serialVersionUID = 4194251899101238989L;

    private String clockId;
    private Date tickTime;

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
}
