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

package org.oxymores.chronix.wapi;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.ConfigNodeBase;
import org.oxymores.chronix.demo.DemoApplication;
import org.oxymores.chronix.dto.DTOChain;
import org.oxymores.chronix.dto.Frontier;
import org.oxymores.chronix.internalapi.IServiceClient;

public class ServiceClient implements IServiceClient {

	@Override
	public String sayHello() {
		
		return "houba hop";
	}

	@Override
	public Application getApplication() {
		// TODO Auto-generated method stub
		return DemoApplication.getNewDemoApplication();
	}

	@Override
	public DTOChain getChain() {
		Application a = DemoApplication.getNewDemoApplication();
		Chain c = null;
		for (ConfigNodeBase cnb : a.getElements())
		{
			if (cnb instanceof Chain)
			{
				c = (Chain)cnb;
				break;
			}
		}
		return Frontier.getChain(c);
	}

}
