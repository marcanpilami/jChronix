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

package org.oxymores.chronix.core.transactional;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;

import org.apache.openjpa.persistence.InverseLogical;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class EventConsumption extends TranscientBase
{
	private static final long serialVersionUID = 4960077419503476652L;

	@ManyToOne(cascade = CascadeType.ALL)
	@InverseLogical("consumptions")
	public Event event;

	public Event getEvent()
	{
		return event;
	}

	public void setEvent(Event event)
	{
		this.event = event;
	}
}
