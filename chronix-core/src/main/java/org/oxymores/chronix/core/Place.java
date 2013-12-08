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

public class Place extends ApplicationObject
{
	private static final long serialVersionUID = 4736385443687921653L;
	
	protected String name, description;
	protected String property1, property2, property3, property4;

	protected ExecutionNode node;
	protected ArrayList<PlaceGroup> memberOfGroups;

	public Place()
	{
		super();
		memberOfGroups = new ArrayList<PlaceGroup>();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getProperty1()
	{
		return property1;
	}

	public void setProperty1(String property1)
	{
		this.property1 = property1;
	}

	public String getProperty2()
	{
		return property2;
	}

	public void setProperty2(String property2)
	{
		this.property2 = property2;
	}

	public String getProperty3()
	{
		return property3;
	}

	public void setProperty3(String property3)
	{
		this.property3 = property3;
	}

	public String getProperty4()
	{
		return property4;
	}

	public void setProperty4(String property4)
	{
		this.property4 = property4;
	}

	public ExecutionNode getNode()
	{
		return node;
	}

	public void setNode(ExecutionNode node)
	{
		if (this.node == null || !this.node.equals(node))
		{
			this.node = node;
			node.addHostedPlace(this);
		}
	}

	public ArrayList<PlaceGroup> getMemberOfGroups()
	{
		return memberOfGroups;
	}

	public void addToGroup(PlaceGroup group)
	{
		if (!this.memberOfGroups.contains(group))
		{
			this.memberOfGroups.add(group);
			group.addPlace(this);
		}
	}
}
