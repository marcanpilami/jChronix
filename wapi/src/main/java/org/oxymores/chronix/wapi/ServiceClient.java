package org.oxymores.chronix.wapi;

import org.oxymores.chronix.internalapi.IServiceClient;

public class ServiceClient implements IServiceClient {

	@Override
	public String sayHello() {
		
		return "houba hop";
	}

}
