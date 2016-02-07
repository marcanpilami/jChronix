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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotNull;

/**
 Base class for all metadata objects.<br>
 Provides an UUID identifier, as well as an ordered list of key/value pairs to be used as environment variables.
 */
public class ChronixObject implements Serializable
{
    private static final long serialVersionUID = 1106120751950998543L;

    @NotNull
    public UUID id;

    @NotNull
    protected ArrayList<EnvironmentParameter> envParams;

    @NotNull
    private List<String> tags;

    public ChronixObject()
    {
        id = UUID.randomUUID();
        envParams = new ArrayList<>();
        tags = new ArrayList<>();
    }

    public UUID getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ChronixObject))
        {
            return false;
        }
        return ((ChronixObject) o).getId().equals(this.getId());
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }

    public boolean validate()
    {
        return false;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public void addEnvVar(String key, String value)
    {
        this.envParams.add(new EnvironmentParameter(key, value));
    }

    public void addInternalEnvVar(String key, String value)
    {
        this.envParams.add(new EnvironmentParameter(key, value, false));
    }

    public void removeEnvVar(String key)
    {
        for (EnvironmentParameter p : envParams)
        {
            if (p.key.equals(key))
            {
                envParams.remove(p);
            }
        }
    }

    public List<EnvironmentParameter> getEnvVars()
    {
        return this.envParams;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }
}
