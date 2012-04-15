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
