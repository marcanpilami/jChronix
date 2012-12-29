package org.oxymores.chronix.wapi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.ResOrder;

@Path("/main")
public interface IServiceConsoleRest
{
	@GET
	@Path("/logs/{a}/{b}/{c}")
	public List<DTORunLog> getLog(@PathParam("a") Date from, @PathParam("b") Date to, @PathParam("c") Date since);

	@GET
	@Path("/logssince/{a}")
	public List<DTORunLog> getLogSince(@PathParam("a") Date since);

	@GET
	@Path("/alllogs")
	@Produces("application/json")
	public ArrayList<DTORunLog> getLog();

	@GET
	@Path("/shortlog/{id}")
	@Produces("text/plain")
	public String getShortLog(@PathParam("id") UUID id);
	
	@GET
	@Path("/order/forceok/{launchId}")
	@Produces("application/json")
	public ResOrder orderForceOK(@PathParam("launchId") String launchId);
}
