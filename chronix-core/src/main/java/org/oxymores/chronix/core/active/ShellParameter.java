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

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Parameter;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.Runner;
import org.oxymores.chronix.engine.data.RunDescription;

public class ShellParameter extends Parameter
{
    private static final long serialVersionUID = 7528888158440570804L;
    private static Logger log = Logger.getLogger(ShellParameter.class);

    @Override
    public void resolveValue(ChronixContext ctx, Runner sender, PipelineJob pj)
    {
        RunDescription rd = new RunDescription();
        rd.setCommand(this.value);
        rd.setMethod(Constants.JD_METHOD_SHELL);
        rd.setHelperExecRequest(true);

        try
        {
            sender.getParameterValue(rd, pj, this.getId());
        }
        catch (JMSException e)
        {
            log.error("Could not ask for parameter resolution due to a communication issue. No retry wil be attempted.", e);
        }
    }
}
