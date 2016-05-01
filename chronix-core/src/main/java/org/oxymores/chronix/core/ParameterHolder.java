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
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.prm.ParameterResolutionRequest;
import org.oxymores.chronix.api.source.DTOParameter;

/**
 * A wrapper for a {@link Parameter} object.
 */
public class ParameterHolder implements Serializable
{
    private static final long serialVersionUID = 8017529181151172909L;

    /**
     * The holder for the parameter DTO object.
     */
    @NotNull
    private DTOParameter prm;

    /**
     * A simple indication - only used when a plugin is missing and we need its name to help the user.
     **/
    @NotNull
    @Size(min = 2)
    private String pluginName;

    /**
     * The plugin associated to the parameter.
     */
    private transient ParameterProvider provider;

    public String getKey()
    {
        return prm.getKey();
    }

    public String getValue(ParameterResolutionRequest request)
    {
        return this.provider.getValue(request);
    }

    public void setDto(DTOParameter dto)
    {
        this.prm = dto;
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
        return prm.getId();
    }

    /**
     * Should only be used inside the parameter request. TODO: check if can go inside right package to scope it more precisely.
     */
    public DTOParameter getRawParameter()
    {
        return this.prm;
    }

    public ParameterProvider getProvider()
    {
        return provider;
    }

    public void setProvider(ParameterProvider provider)
    {
        this.provider = provider;
    }

    public String getProviderClassName()
    {
        return this.prm.getProviderName();
    }
}
