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

import javax.jms.JMSException;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 A base implementation (simple key/value pair) for parameters. Can be use directly or overloaded.
 */
public class Parameter extends ApplicationObject
{
    private static final Logger log = LoggerFactory.getLogger(Parameter.class);
    private static final long serialVersionUID = 8017529181151172909L;

    @NotNull
    @Size(min = 0, max = 50)
    protected String key;

    @NotNull
    @Size(min = 1, max = 255)
    protected String value;

    @NotNull
    @Size(min = 1, max = 255)
    protected String description;

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

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public void resolveValue(ChronixContext ctx, RunnerManager sender, PipelineJob pj)
    {
        try
        {
            sender.sendParameterValue(this.getValue(), this.getId(), pj);
        }
        catch (JMSException e)
        {
            log.error("Could not send dynamic parameter value", e);
        }
    }
}
