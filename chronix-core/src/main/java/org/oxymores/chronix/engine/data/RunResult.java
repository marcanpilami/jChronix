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
package org.oxymores.chronix.engine.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

import org.joda.time.DateTime;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.api.source.JobDescription;

public class RunResult implements Serializable
{
    private static final long serialVersionUID = 316559310140465996L;

    public RunResult()
    {}

    public RunResult(JobDescription jd)
    {
        this.launchId = jd.getLaunchId();
        this.start = jd.getVirtualTimeStart();
        this.end = null;
    }

    public RunResult(JobDescription jd, EventSourceRunResult r)
    {
        this(jd);

        if (r != null)
        {
            this.end = r.end;
            this.fullerLog = r.fullerLog;
            this.logPath = r.logPath;
            this.logSizeBytes = r.logSizeBytes;
            this.logStart = r.logStart;
            this.newEnvVars = new HashMap<>(r.newEnvVars);
            this.returnCode = r.returnCode;

            if (r.overloadedLaunchId != null)
            {
                this.launchId = r.overloadedLaunchId;
            }
            this.overloadedScopeId = r.overloadedScopeId;
            if (r.overloadedSequenceOccurrence != null)
            {
                this.calendarOverload = r.overloadedSequenceOccurrence;
            }

            if (r.success != null)
            {
                this.success = r.success;
            }
            else
            {
                this.success = r.returnCode == 0;
            }

        }
    }

    public String logStart = "";
    public String fullerLog = "";
    public String logPath = "";
    public String logFileName = "";
    public Long logSizeBytes = null;
    public int returnCode = -1;
    public String conditionData2 = null;
    public String conditionData3 = null;
    public UUID conditionData4 = null;
    public boolean success;
    public HashMap<String, String> newEnvVars = new HashMap<>();
    public DateTime start = DateTime.now(), end = DateTime.now();
    public String envtUser, envtServer, envtOther;
    public String calendarOverload = null;

    // Data below is from and for the engine - not created by the run
    public UUID launchId = null;
    public UUID overloadedScopeId = null;
    public Boolean outOfPlan = false;

    public DateTime nextRun = null;
}
