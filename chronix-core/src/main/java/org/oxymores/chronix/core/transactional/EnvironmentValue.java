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
import java.util.Objects;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import org.sql2o.Connection;

public class EnvironmentValue implements Serializable
{
    private static final long serialVersionUID = -3301527648471127170L;

    // UUID and not Integer => we may need to transmit this to other nodes
    @NotNull
    private UUID id;

    @NotNull
    @Size(min = 1, max = 50)
    private String key;

    @NotNull
    @Size(min = 0, max = 255)
    private String value;

    @NotNull
    private UUID transientID;

    public EnvironmentValue()
    {
        id = UUID.randomUUID();
    }

    public EnvironmentValue(String key, String value, TranscientBase tb)
    {
        id = UUID.randomUUID();
        this.key = key;
        this.value = value;
        this.transientID = tb.getId();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof EnvironmentValue))
        {
            return false;
        }
        return ((EnvironmentValue) o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.id);
        return hash;
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public UUID getTransientID()
    {
        return transientID;
    }

    public void setTransientID(UUID transientID)
    {
        this.transientID = transientID;
    }

    public void insert(Connection conn)
    {
        conn.createQuery("INSERT INTO EnvironmentValue(id, key, transientID, value) "
                + "VALUES(:id, :key, :transientID, :value)").bind(this).executeUpdate();
    }
}
