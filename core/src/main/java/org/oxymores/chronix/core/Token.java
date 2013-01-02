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
import java.util.List;

public class Token extends ApplicationObject
{
	private static final long serialVersionUID = 6422487791877618666L;

	String name;
	int count = 1;
	boolean byPlace = false;

	protected ArrayList<State> usedInStates;

	// Constructor
	public Token()
	{
		super();
		usedInStates = new ArrayList<State>();
	}

	// /////////////////////////////////////////////////
	// Stupid GET/SET
	public boolean isByPlace()
	{
		return byPlace;
	}

	public void setByPlace(boolean byPlace)
	{
		this.byPlace = byPlace;
	}

	public int getCount()
	{
		return count;
	}

	public void setCount(int count)
	{
		this.count = count;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	//
	// /////////////////////////////////////////////////

	// /////////////////////////////////////////////////
	// Relationships
	public List<State> getUsedInStates()
	{
		return this.usedInStates;
	}

	// Only called from State.addToken
	void s_addStateUsing(State s)
	{
		usedInStates.add(s);
	}

	// Only called from State.addToken
	void s_removeStateUsing(State s)
	{
		try
		{
			usedInStates.remove(s);
		} finally
		{ // do nothing if asked to remove a non existent state
		}
	}
	// relationships
	// /////////////////////////////////////////////////
}
