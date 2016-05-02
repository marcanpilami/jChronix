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

import java.util.ArrayList;

import org.oxymores.chronix.core.NamedApplicationObject;

public class AutoSequence extends NamedApplicationObject
{
    private static final long serialVersionUID = -4652472532871950327L;

    protected ArrayList<State> usedInStates;

    public AutoSequence()
    {
        super();
        usedInStates = new ArrayList<State>();
    }

    // Only called from State.addSequence
    void s_addStateUsing(State s)
    {
        usedInStates.add(s);
    }

    // Only called from State.addSequence
    void s_removeStateUsing(State s)
    {
        try
        {
            usedInStates.remove(s);
        }
        finally
        {
            // do nothing if asked to remove a non existent state
        }
    }
}
