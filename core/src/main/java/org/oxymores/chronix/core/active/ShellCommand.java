/**
 * @author Marc-Antoine Gouillart
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
import javax.persistence.EntityManager;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunDescription;
import org.oxymores.chronix.engine.Runner;

public class ShellCommand extends ActiveNodeBase {

	private static final long serialVersionUID = 3340501935290198518L;

	protected String command;

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	private String resolveCommand() {
		return command;
	}

	@Override
	public void run(PipelineJob p, Runner sender, ChronixContext ctx,
			EntityManager em) {
		super.run(p, sender, ctx, em);

		RunDescription rd = new RunDescription();
		rd.command = resolveCommand();

		try {
			sender.sendRunDescription(rd, p.getPlace(ctx), p);
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String getActivityMethod() {
		return "Shell";
	}

	@Override
	public String getCommandName(PipelineJob pj, Runner sender,
			ChronixContext ctx) {
		return this.command;
	}

	@Override
	public boolean hasPayload() {
		return true;
	}
}
