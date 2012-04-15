package org.oxymores.chronix.wapi;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.oxymores.chronix.internalapi.IServiceClient;

public class ClientClient implements IServiceClient {
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
	public String sayHello() {
		return proxy.sayHello();
	}
	

}
