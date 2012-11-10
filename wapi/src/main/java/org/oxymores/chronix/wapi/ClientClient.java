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

import java.util.Date;
import java.util.List;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.oxymores.chronix.dto.DTOApplication;
import org.oxymores.chronix.dto.DTOChain;
import org.oxymores.chronix.dto.DTORRule;
import org.oxymores.chronix.internalapi.IServiceClient;

public class ClientClient implements IServiceClient
{
	IServiceClient proxy;

	public ClientClient()
	{
		this("http://localhost:9000/Hello");
	}

	public ClientClient(String url)
	{
		ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
		factory.setAddress(url);
		factory.getServiceFactory().setDataBinding(new AegisDatabinding());
		proxy = factory.create(IServiceClient.class);
	}

	@Override
	public String sayHello()
	{
		return proxy.sayHello();
	}

	@Override
	public DTOApplication getApplication(String name)
	{
		return null;
	}

	// @Override
	public DTOApplication getApplication(String name, Boolean byUuid)
	{
		return null;
	}

	@Override
	public DTOChain getChain()
	{
		return proxy.getChain();
	}

	@Override
	public void stageApplication(DTOApplication app)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void storeApplication(String uuid)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void resetStage()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public List<Date> getNextRRuleOccurrences(DTORRule rule, String low, String high)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
