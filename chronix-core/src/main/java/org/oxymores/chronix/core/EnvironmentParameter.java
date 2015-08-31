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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public class EnvironmentParameter
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
        super();
        this.key = key;
        this.value = value;
    }
}
