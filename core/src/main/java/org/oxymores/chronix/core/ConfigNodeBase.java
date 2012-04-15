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

package org.oxymores.chronix.core;

import java.util.ArrayList;

public class ConfigNodeBase extends MetaObject {
	private static final long serialVersionUID = 4288408733877784921L;
	
	protected Application Application;
	protected ArrayList<Parameter> parameters;
	
	public ConfigNodeBase()
	{
		super();
		parameters = new ArrayList<Parameter>();
	}
	
	
	public Application getApplication() {
		return Application;
	}
	public void setApplication(Application application) {
		Application = application;
	}
	
	public ArrayList<Parameter> getParameters()
	{
		return this.parameters;
	}
	
	public void addParameter(Parameter parameter)
	{
		if (! parameters.contains(parameter))
		{
			parameters.add(parameter);
			parameter.addElement(this);
		}
	}
}
