package org.oxymores.chronix.wapi;

import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.oxymores.chronix.internalapi.IServiceClient;

public class TestHelpers
{
    static IServiceClient getDevProxy()
    {
        ClientProxyFactoryBean factory = new ClientProxyFactoryBean();
        factory.setAddress("http://localhost:9000/Hello");
        factory.getServiceFactory().setDataBinding(new AegisDatabinding());
        return factory.create(IServiceClient.class);
    }
}
