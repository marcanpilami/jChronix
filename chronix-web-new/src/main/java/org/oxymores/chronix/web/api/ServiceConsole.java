package org.oxymores.chronix.web.api;

import java.io.File;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;
import org.oxymores.chronix.core.engine.api.HistoryService;
import org.oxymores.chronix.core.engine.api.OrderService;
import org.oxymores.chronix.dto.HistoryQuery;
import org.oxymores.chronix.dto.ResOrder;
import org.oxymores.chronix.engine.modularity.web.RestServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ServiceConsole.class)
@JaxrsResource
@Path("/live")
public class ServiceConsole implements RestServiceApi
{
    private static final Logger log = LoggerFactory.getLogger(ServiceConsole.class);

    private @Context HttpServletResponse response;
    HistoryService apiHistory;
    OrderService apiOrder;

    @Reference
    public void setApiHistory(HistoryService api)
    {
        this.apiHistory = api;
    }

    @Reference
    public void setApiOrder(OrderService api)
    {
        this.apiOrder = api;
    }

    public ServiceConsole()
    {
        // OSGI constructor
    }

    ServiceConsole(HistoryService api1, OrderService api2)
    {
        this.apiHistory = api1;
        this.apiOrder = api2;
    }

    @POST
    @Path("log")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HistoryQuery getLog(HistoryQuery q)
    {
        log.debug("getLog POST was called");
        return apiHistory.query(q);
    }

    @GET
    @Path("/shortlog/{id}")
    @Produces("text/plain")
    public String getShortLog(@PathParam("id") UUID id)
    {
        log.debug("Service getShortLog was called for ID " + id.toString());
        return apiHistory.getShortLog(id);
    }

    @GET
    @Path("/logfile/{launchId}")
    @Produces("text/plain; charset=utf-8")
    public File getLogFile(@PathParam("launchId") UUID launchId)
    {
        File res = apiHistory.getLogFile(launchId);
        response.setHeader("Content-Disposition", "attachment; filename=" + res.getName());
        return res;
    }

    @GET
    @Path("/order/forceok/{launchId}")
    @Produces("application/json")
    public ResOrder orderForceOK(@PathParam("launchId") UUID launchId)
    {
        log.debug("Service orderForceOK was called");
        return apiOrder.orderForceOK(launchId);
    }

    @GET
    @Path("/order/launch/{insidePlan}/{appId}/{stateId}/{placeId}")
    @Produces("application/json")
    public ResOrder orderLaunch(@PathParam("appId") UUID appId, @PathParam("stateId") UUID stateId, @PathParam("placeId") UUID placeId,
            @PathParam("insidePlan") Boolean insidePlan)
    {
        log.info("Calling orderLaunchOutOfPlan - with full params");
        return apiOrder.orderLaunch(appId, stateId, placeId, insidePlan);
    }

    @GET
    @Path("/order/launch/outofplan/duplicatelaunch/{launchId}")
    @Produces("application/json")
    public ResOrder duplicateEndedLaunchOutOfPlan(@PathParam("launchId") UUID launchId)
    {
        log.debug("Service duplicateEndedLaunchOutOfPlan was called");
        return apiOrder.duplicateEndedLaunchOutOfPlan(launchId);
    }
}
