package org.oxymores.chronix.web;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Path("/meta")
@Component(service = ServiceMetaNetwork.class)
@JaxrsResource
public class ServiceMetaNetwork
{
    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public String test()
    {
        return "OK";
    }
}
