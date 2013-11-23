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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class EnvironmentValue implements Serializable
{
    private static final long serialVersionUID = -3301527648471127170L;

    @Id
    @Column(nullable = false, length = 36)
    private String id;
    @Column(length = 50)
    private String key, value;

    public EnvironmentValue()
    {
        id = UUID.randomUUID().toString();
    }

    public EnvironmentValue(String key, String value)
    {
        id = UUID.randomUUID().toString();
        this.key = key;
        this.value = value;
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
        return UUID.fromString(id).hashCode();
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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
}
