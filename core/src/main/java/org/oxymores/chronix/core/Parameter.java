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

public class Parameter extends MetaObject {

	private static final long serialVersionUID = 8017529181151172909L;

	protected String key, value, description;
	protected Boolean reusable = false;
	
	protected Application application;
	protected ArrayList<ConfigNodeBase> elements;
	
	public Parameter()
	{
		super();
		elements = new ArrayList<ConfigNodeBase>();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getReusable() {
		return reusable;
	}

	public void setReusable(Boolean reusable) {
		this.reusable = reusable;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public ArrayList<ConfigNodeBase> getElements() {
		return elements;
	}
	
	public void addElement(ConfigNodeBase element)
	{
		if (!elements.contains(element))
		{
			elements.add(element);
			element.addParameter(this);
		}
	}
}
