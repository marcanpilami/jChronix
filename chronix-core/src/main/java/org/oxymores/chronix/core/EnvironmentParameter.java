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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.EnvironmentValue;

public class EnvironmentParameter implements Serializable
{
    private static final long serialVersionUID = -3573665084125546426L;

    @NotNull
    @Size(min = 1, max = 50)
    protected String key;

    @NotNull
    @Size(min = 0, max = 255)
    String value;

    boolean addToRunEnvironment = true;

    public EnvironmentParameter(String key, String value)
    {
        this.key = key;
        this.value = value;
    }

    public EnvironmentParameter(String key, String value, boolean addToRunEnvironment)
    {
        this.key = key;
        this.value = value;
        this.addToRunEnvironment = addToRunEnvironment;
    }

    /**
     * Full environment, without taking into account addToRunEnvironment
     * 
     * @param s
     * @param p
     * @param params
     * @return
     */
    public static Map<String, String> resolveEnvironment(State s, Place p, EnvironmentValue... params)
    {
        Map<String, String> res = new HashMap<>();

        // Overload priority is: Explicit > State > Chain > ActiveSource > PlaceGroup > Application > Place > NetworkNode.
        // Auto variables are reset each time and always have priority over others.
        addAllToRes(res, p.getNode().getEnvVars());
        addAllToRes(res, p.getEnvVars());
        // addAllToRes(res, s.getApplication().getEnvVars());
        addAllToRes(res, s.getRunsOn().getEnvVars());
        // addAllToRes(res, s.getRepresents().getEnvVars());
        // addAllToRes(res, s.getChain().getEnvVars());
        addAllToRes(res, s.getEnvVars());
        for (EnvironmentValue ev : params)
        {
            if (ev.getKey().startsWith("CHR_"))
            {
                // Don't propagate auto variables
                continue;
            }
            res.put(ev.getKey(), ev.getValue());
        }

        return res;
    }

    /**
     * The environment that should be exposed to the runtime environment (i.e. shell variables, etc.)
     * 
     * @param s
     * @param p
     * @param params
     * @return
     */
    public static Map<String, String> resolveRuntimeEnvironment(State s, Place p, EnvironmentValue... params)
    {
        Map<String, String> res = new HashMap<>();

        // Overload priority is: Explicit > State > Chain > ActiveSource > PlaceGroup > Application > Place > NetworkNode.
        // Auto variables are reset each time and always have priority over others.
        addAllPublicToRes(res, p.getNode().getEnvVars());
        addAllPublicToRes(res, p.getEnvVars());
        // addAllPublicToRes(res, s.getApplication().getEnvVars());
        addAllPublicToRes(res, s.getRunsOn().getEnvVars());
        // addAllPublicToRes(res, s.getRepresents().getEnvVars());
        // addAllPublicToRes(res, s.getChain().getEnvVars());
        addAllPublicToRes(res, s.getEnvVars());
        for (EnvironmentValue ev : params)
        {
            if (ev.getKey().startsWith("CHR_"))
            {
                // Don't propagate auto variables
                continue;
            }
            res.put(ev.getKey(), ev.getValue());
        }

        return res;
    }

    private static void addAllToRes(Map<String, String> res, List<EnvironmentParameter> eps)
    {
        for (EnvironmentParameter ep : eps)
        {
            res.put(ep.key, ep.value);
        }
    }

    private static void addAllPublicToRes(Map<String, String> res, List<EnvironmentParameter> eps)
    {
        for (EnvironmentParameter ep : eps)
        {
            if (ep.addToRunEnvironment)
            {
                res.put(ep.key, ep.value);
            }
            else
            {
                // A private overload to a public variable makes it private.
                res.remove(ep.key);
            }
        }
    }
}
