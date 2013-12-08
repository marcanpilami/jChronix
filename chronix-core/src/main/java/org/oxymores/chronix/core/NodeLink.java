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

public class NodeLink extends ApplicationObject
{
	private static final long serialVersionUID = 7100333809406249831L;
	
	protected NodeConnectionMethod method;
	protected ExecutionNode nodeFrom, nodeTo;

	public NodeConnectionMethod getMethod()
	{
		return method;
	}

	public void setMethod(NodeConnectionMethod method)
	{
		this.method = method;
	}

	public ExecutionNode getNodeFrom()
	{
		return nodeFrom;
	}

	public void setNodeFrom(ExecutionNode nodeFrom)
	{
		if (this.nodeFrom == null || this.nodeFrom != nodeFrom)
		{
			this.nodeFrom = nodeFrom;
			nodeFrom.addCanSendTo(this);
		}
		else
			this.nodeFrom = nodeFrom;
	}

	public ExecutionNode getNodeTo()
	{
		return nodeTo;
	}

	public void setNodeTo(ExecutionNode nodeTo)
	{
		if (this.nodeTo == null || this.nodeTo != nodeTo)
		{
			this.nodeTo = nodeTo;
			nodeTo.addCanReceiveFrom(this);
		}
		else
			this.nodeTo = nodeTo;
	}
}
