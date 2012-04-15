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

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.oxymores.chronix.internalapi.IServer;
import org.oxymores.chronix.internalapi.IServiceClient;
import org.apache.cxf.endpoint.Server;

public class JettyServer implements IServer {
    
	protected Server server;
	protected String interfaceToListenOn;
	protected Integer portToListenOn;
	
	public JettyServer() {
		this("localhost", 9000);
    }
	public JettyServer(String hostname, Integer Port) {
		this.interfaceToListenOn = hostname;
		this.portToListenOn = Port;
    }

	private String getURL()
	{
		return "http://" + this.interfaceToListenOn + ":" + this.portToListenOn + "/Hello";
	}
    
    @Override
    public void start() {
        ServiceClient serviceImpl = new ServiceClient();
        ServerFactoryBean svrFactory = new ServerFactoryBean();
        svrFactory.setServiceClass(IServiceClient.class);
        svrFactory.setAddress(this.getURL());
        svrFactory.setServiceBean(serviceImpl);
        svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());
        
        server = svrFactory.create();
    }
    
    @Override
    public void stop()
    {
    	server.stop();
    }

}
