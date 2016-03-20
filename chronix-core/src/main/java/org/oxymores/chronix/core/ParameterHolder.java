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

import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.api.prm.ParameterProvider;

/**
 * A wrapper for a {@link Parameter} object.
 */
public class ParameterHolder extends ChronixObject
{
    private static final long serialVersionUID = 8017529181151172909L;

    @NotNull
    @Size(min = 0, max = 50)
    protected String key;

    @NotNull
    @Size(min = 1, max = 255)
    protected String description;

    @NotNull
    protected transient Parameter prm;

    private UUID dtoId;

    // A simple indication - only used when a plugin is missing and we need its name to help the user.
    private String pluginName;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getValue(String replyQueueName, String prmLaunchId)
    {
        return this.prm.getValue(replyQueueName, prmLaunchId);
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void setDto(Parameter dto)
    {
        this.prm = dto;
        this.dtoId = dto.getId();
    }

    public String getPluginName()
    {
        return pluginName;
    }

    public void setPluginName(String pluginName)
    {
        this.pluginName = pluginName;
    }

    public UUID getParameterId()
    {
        return this.dtoId;
    }

    public Class<? extends ParameterProvider> getProviderClass()
    {
        return this.prm.getProvider();
    }

    public Parameter getDto()
    {
        return this.prm;
    }
}
