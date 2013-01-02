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

package org.oxymores.chronix.core.active;

import javax.jms.JMSException;

import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunDescription;
import org.oxymores.chronix.engine.Runner;

public class ShellParameter extends Parameter
{
	private static final long serialVersionUID = 7528888158440570804L;

	@Override
	public void resolveValue(ChronixContext ctx, Runner sender, PipelineJob pj)
	{
		RunDescription rd = new RunDescription();
		rd.command = this.value;
		rd.Method = "Shell";
		rd.helperExecRequest = true;

		try
		{
			sender.getParameterValue(rd, pj, this.getId());
		} catch (JMSException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
