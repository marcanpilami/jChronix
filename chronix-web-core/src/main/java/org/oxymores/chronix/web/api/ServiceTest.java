package org.oxymores.chronix.web.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;

@Component
@Path("test")
public class ServiceTest implements RestServiceApi
{
    @GET
    public String test()
    {
        return "OK";
    }
}
