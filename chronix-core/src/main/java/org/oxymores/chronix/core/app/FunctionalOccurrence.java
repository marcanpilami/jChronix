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
import java.util.UUID;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * An occurrence inside a {@link FunctionalSequence}. All in all, a simple text value that is made available inside the launch environment.
 */
public class FunctionalOccurrence implements Serializable
{
    private static final long serialVersionUID = -8296932253108182976L;

    @NotNull
    @Size(min = 1, max = 255)
    public String label;

    @NotNull
    private UUID id;

    public FunctionalOccurrence(String label)
    {
        this.label = label;
        this.id = UUID.randomUUID();
    }

    public String getLabel()
    {
        return label;
    }

    public UUID getId()
    {
        return this.id;
    }
}
