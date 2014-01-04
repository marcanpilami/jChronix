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

import java.util.ArrayList;

import javax.validation.constraints.NotNull;

public class ConfigurableBase extends ApplicationObject
{
    private static final long serialVersionUID = 4288408733877784921L;

    @NotNull
    protected ArrayList<Parameter> parameters;
    @NotNull
    protected ArrayList<EnvironmentParameter> envParams;

    public ConfigurableBase()
    {
        super();
        parameters = new ArrayList<Parameter>();
        envParams = new ArrayList<EnvironmentParameter>();
    }

    public ArrayList<Parameter> getParameters()
    {
        return this.parameters;
    }

    public void addParameter(Parameter parameter)
    {
        if (!parameters.contains(parameter))
        {
            parameters.add(parameter);
            parameter.addElement(this);
        }
    }

    public void addParameter(String key, String value, String description)
    {
        Parameter p = new Parameter();
        p.setDescription(description);
        p.setKey(key);
        p.setReusable(false);
        p.setValue(value);
        this.application.addParameter(p);
        addParameter(p);
    }

    public void addEnvVar(String key, String value)
    {
        this.envParams.add(new EnvironmentParameter(key, value));
    }

    public void removeEnvVar(String key)
    {
        for (EnvironmentParameter p : envParams)
        {
            if (p.key == key)
                envParams.remove(p);
        }
    }
}
