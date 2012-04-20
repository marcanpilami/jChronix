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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.DestinationFactoryManagerImpl;
import org.apache.cxf.transport.http_jetty.JettyDestinationFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ServerEngine;
import org.oxymores.chronix.internalapi.IServer;
import org.oxymores.chronix.internalapi.IServiceClient;
import org.apache.cxf.endpoint.Server;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.FileResource;

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
        
        //svrFactory.getServiceFactory().getBus().setExtension(new org.apache.cxf.javascript.JavascriptQueryHandler(svrFactory.getServiceFactory().getBus()), org.apache.cxf.javascript.JavascriptQueryHandler.class );
       
        try {
        	String address = svrFactory.getAddress();

        	Bus _defaultBUS = BusFactory.getDefaultBus();
        //	JettyHTTPServerEngineFactory jettyFactory = _defaultBUS.getExtension(JettyHTTPServerEngineFactory.class);
        	
        	EndpointInfo ei = new EndpointInfo();
	        ei.setAddress(address);
	        
	        DestinationFactoryManager dfm = _defaultBUS.getExtension(DestinationFactoryManager.class);
	        DestinationFactory df = dfm.getDestinationFactoryForUri(address);
	        
        	//DestinationFactory df =  svrFactory.getDestinationFactory();
	        JettyHTTPDestination destination = (JettyHTTPDestination) df.getDestination(ei);
	        ServerEngine engine = destination.getEngine();
	        Handler handler = engine.getServant(new URL(address));
			org.eclipse.jetty.server.Server server = handler.getServer(); // The Server
			
			
			// Add a handler for static content
			Handler serverHandler = server.getHandler();
			HandlerList handlerList = new HandlerList();
		    ResourceHandler resourceHandler = new ResourceHandler();
		    handlerList.addHandler(resourceHandler);
		    handlerList.addHandler(serverHandler);
		    
		    server.setHandler(handlerList);
		    
		    // Configure static content
		    File staticContentFile = new File("C:\\Users\\mag\\jChronix\\gui\\index.html"); // ordinary pathname.
		    URL targetURL = new URL("file://" + staticContentFile.getCanonicalPath());
		    FileResource fileResource = new FileResource(targetURL);
		    resourceHandler.setBaseResource(fileResource);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        server = svrFactory.create();
        
    }
    
    @Override
    public void stop()
    {
    	server.stop();
    }

}
