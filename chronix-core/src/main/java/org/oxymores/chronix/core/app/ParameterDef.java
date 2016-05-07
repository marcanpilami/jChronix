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
package org.oxymores.chronix.core.app;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.osgi.framework.FrameworkUtil;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.transactional.ParameterResolutionRequest;

/**
 * A wrapper for a {@link Parameter} object.
 */
public class ParameterDef implements Serializable
{
    private static final long serialVersionUID = 8017529181151172909L;

    /////////////////////////////////
    // Identity

    /**
     * Unique ID
     */
    private final UUID id;

    /**
     * Optional for additional parameters. The "applicative key" of the parameter".
     */
    private String key;

    /////////////////////////////////
    // Direct value fields

    private String value;

    /////////////////////////////////
    // Reference fields

    /**
     * Note that we store the reference here - it is resolved at runtime. This simplifies deserialisation.
     */
    private UUID prmReference = null;

    /////////////////////////////////
    // Dynamically resolved fields

    private String providerClassName;
    private Map<String, ParameterDef> fields = null;
    private List<ParameterDef> additionalParameters = null;

    /////////////////////////////////
    // Engine helpers

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

    ///////////////////////////////////////////////////////////////////////////
    // Construction & Deserialisation
    ///////////////////////////////////////////////////////////////////////////

    public ParameterDef(DTOParameter dto, Application a, ChronixContextMeta ctx)
    {
        this.id = dto.getId();
        this.key = dto.getKey();
        this.value = dto.getDirectValue();
        this.prmReference = dto.getReference();
        this.providerClassName = dto.getProviderName();
        if (this.providerClassName != null)
        {
            this.fields = new HashMap<>(dto.getFields().size());
            for (DTOParameter subPrm : dto.getFields().values())
            {
                this.fields.put(subPrm.getKey(), new ParameterDef(subPrm, a, ctx));
            }

            this.additionalParameters = new ArrayList<>(dto.getAdditionalParameters().size());
            for (DTOParameter subPrm : dto.getAdditionalParameters())
            {
                this.additionalParameters.add(new ParameterDef(subPrm, a, ctx));
            }

            this.provider = ctx.getParameterProvider(this.providerClassName);
            this.pluginName = FrameworkUtil.getBundle(this.provider.getClass()).getBundleContext().getBundle().getSymbolicName();
        }
    }

    public void setProvider(ParameterProvider provider)
    {
        this.provider = provider;
    }

    ///////////////////////////////////////////////////////////////////////////
    // DTO
    ///////////////////////////////////////////////////////////////////////////

    public DTOParameter getDTO()
    {
        if (this.value != null)
        {
            return new DTOParameter(key, value).setId(id);
        }
        else if (this.prmReference != null)
        {
            return new DTOParameter(key, prmReference).setId(id);
        }
        else
        {
            DTOParameter res = new DTOParameter(key, provider).setId(id);
            for (ParameterDef p : this.additionalParameters)
            {
                res.addAdditionalarameter(p.getDTO());
            }
            for (ParameterDef p : this.fields.values())
            {
                res.setField(p.getDTO());
            }
            return res;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Engine methods
    ///////////////////////////////////////////////////////////////////////////

    public String getValue(ParameterResolutionRequest request)
    {
        if (request.getReferencedValue() != null)
        {
            return request.getReferencedValue();
        }
        return this.provider.getValue(request);
    }

    public ParameterProvider getProvider()
    {
        return provider;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parameter access
    ///////////////////////////////////////////////////////////////////////////

    public List<ParameterDef> getAllParameters()
    {
        List<ParameterDef> res = new ArrayList<>(this.additionalParameters);
        res.addAll(this.fields.values());
        return res;
    }

    public List<ParameterDef> getAdditionalParameters()
    {
        return this.additionalParameters;
    }

    public Collection<ParameterDef> getFields()
    {
        return this.fields.values();
    }

    /**
     * null if not found.
     * 
     * @param key
     * @return
     */
    public ParameterDef getField(UUID key)
    {
        for (ParameterDef ph : this.fields.values())
        {
            if (ph.getParameterId().equals(key))
            {
                return ph;
            }
        }
        return null;
    }

    /**
     * null if not found.
     * 
     * @param key
     * @return
     */
    public ParameterDef getAdditionalParameter(UUID key)
    {
        for (ParameterDef ph : this.additionalParameters)
        {
            if (ph.getParameterId().equals(key))
            {
                return ph;
            }
        }
        return null;
    }

    public List<ParameterDef> getSubParametersOfType(String serviceClassName)
    {
        List<ParameterDef> res = new ArrayList<>();

        if (this.providerClassName != null)
        {
            if (this.providerClassName.equals(serviceClassName))
            {
                res.add(this);
            }

            for (ParameterDef ph : this.getAllParameters())
            {
                res.addAll(ph.getSubParametersOfType(serviceClassName));
            }
            return res;
        }
        return res;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stupid get/set
    ///////////////////////////////////////////////////////////////////////////

    public String getKey()
    {
        return this.key;
    }

    public String getPluginName()
    {
        return pluginName;
    }

    public UUID getParameterId()
    {
        return this.id;
    }

    public String getProviderClassName()
    {
        return this.providerClassName;
    }

    public String getDirectValue()
    {
        return this.value;
    }

    public UUID getReference()
    {
        return this.prmReference;
    }
}
