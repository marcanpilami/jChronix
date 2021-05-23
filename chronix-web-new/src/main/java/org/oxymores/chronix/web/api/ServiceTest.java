package org.oxymores.chronix.web.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;

@Component(service = ServiceTest.class)
@JaxrsResource
@Path("/test")
public class ServiceTest implements RestServiceApi
{
    @GET
    public String test()
    {
        return "OK";
    }
}
