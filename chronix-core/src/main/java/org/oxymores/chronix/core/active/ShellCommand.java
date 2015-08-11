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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;

public class ShellCommand extends ActiveNodeBase
{
    private static final long serialVersionUID = 3340501935290198518L;

    @NotNull
    @Size(min = 1, max = 255)
    protected String command;

    protected String shell = "cmd.exe";

    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        this.command = command;
    }

    @Override
    public String getActivityMethod()
    {
        return "Shell";
    }

    @Override
    public String getSubActivityMethod()
    {
        return this.shell;
    }

    @Override
    public String getCommandName(PipelineJob pj, RunnerManager sender, ChronixContext ctx)
    {
        return this.command;
    }

    @Override
    public boolean hasExternalPayload()
    {
        return true;
    }

    public String getShell()
    {
        return shell;
    }

    public void setShell(String shell)
    {
        this.shell = shell;
    }
}
