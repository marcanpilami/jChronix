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

package org.oxymores.chronix.engine;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class RunResult implements Serializable
{
	private static final long serialVersionUID = 316559310140465996L;

	public String logStart = "";
	public String fullerLog = "";
	public String logPath = "";
	public String logFileName = "";
	public long logSizeBytes = 0;
	public int returnCode = -1;
	public String conditionData2 = null;
	public String conditionData3 = null;
	public String conditionData4 = null; // Actually UUID
	public HashMap<String, String> newEnvVars = new HashMap<String, String>();
	public Date start, end;
	public String envtUser, envtServer, envtOther;

	// Data below is from and for the engine - not created by the run
	public String id1 = null;
	public UUID id2 = null;
	public Boolean outOfPlan = false;
}
