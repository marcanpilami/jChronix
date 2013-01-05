package org.oxymores.chronix.wapi;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.Frontier;
import org.oxymores.chronix.dto.ResOrder;
import org.oxymores.chronix.engine.SenderHelpers;
import org.oxymores.chronix.exceptions.ChronixException;

public class ServiceConsole implements IServiceConsoleSoap, IServiceConsoleRest
{
	private static Logger log = Logger.getLogger(ServiceConsole.class);

	private ChronixContext ctx = null;
	private EntityManagerFactory emfHistory;

	public ServiceConsole(ChronixContext ctx)
	{
		this.ctx = ctx;
		this.emfHistory = this.ctx.getHistoryEMF();
	}

	@Override
	public List<DTORunLog> getLog(Date from, Date to, Date since)
	{
		log.debug(String.format("Service getLog was called between %s and %s (received since %s)", from, (since.after(from) ? new Date()
				: to), since));
		EntityManager em = emfHistory.createEntityManager();
		TypedQuery<RunLog> q = em
				.createQuery(
						"SELECT r FROM RunLog r WHERE r.markedForUnAt >= ?1 AND r.markedForUnAt <= ?2 AND r.lastLocallyModified >= ?3 ORDER BY r.markedForUnAt",
						RunLog.class);
		q.setParameter(1, from);
		q.setParameter(2, (since.after(from) ? new Date() : to)); // if since is used, the end should always be now
		q.setParameter(3, since);
		ArrayList<DTORunLog> res = new ArrayList<DTORunLog>();

		for (RunLog rl : q.getResultList())
			res.add(Frontier.getDTORunLog(rl));

		log.debug("End of call to getLog - returning " + res.size() + " logs");
		return res;
	}

	@Override
	public ArrayList<DTORunLog> getLog()
	{
		log.debug("Service getLog was called");
		EntityManager em = emfHistory.createEntityManager();
		TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
		ArrayList<DTORunLog> res = new ArrayList<DTORunLog>();

		for (RunLog rl : q.getResultList())
			res.add(Frontier.getDTORunLog(rl));

		log.debug("End of call to getLog - returning " + res.size());
		return res;
	}

	@Override
	public String getShortLog(UUID id)
	{
		log.debug("Service getShortLog was called for ID " + id.toString());
		EntityManager em = emfHistory.createEntityManager();
		RunLog rl = em.find(RunLog.class, id);

		if (rl == null)
		{
			log.debug("Service getShortLog has ended without finding the log");
			return "notfound";
		}
		else
		{
			log.debug("Service getShortLog has ended - the log was found");
			return rl.shortLog.substring(Math.min(rl.shortLog.length(),  2), rl.shortLog.length());
		}
	}

	@Override
	public List<DTORunLog> getLogSince(Date since)
	{
		log.debug("Service getLogSince was called");
		EntityManager em = emfHistory.createEntityManager();
		TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r WHERE r.lastLocallyModified > ? ORDER BY r.enteredPipeAt",
				RunLog.class);
		q.setParameter(0, since);
		ArrayList<DTORunLog> res = new ArrayList<DTORunLog>();

		for (RunLog rl : q.getResultList())
			res.add(Frontier.getDTORunLog(rl));

		log.debug("End of call to getLogSince - returning " + res.size());
		return res;
	}

	@Override
	public ResOrder orderForceOK(String launchId) 
	{
		try {
			EntityManager em = emfHistory.createEntityManager();
			RunLog rl = em.find(RunLog.class, launchId);
			SenderHelpers.sendOrderForceOk(rl.applicationId, rl.id, rl.executionNodeId, ctx);
		} catch (ChronixException e) {
			return new ResOrder("ForceOK", false, e.toString());
		}
		return new ResOrder("ForceOK", true, "The order was sent successfuly");
	}

	@Override
	@GET
	@Path("/logfile/{launchId}")
	@Produces("text/plain; charset=utf-8")
	public File getLogFile(@PathParam("launchId") String launchId) 
	{
		EntityManager em = emfHistory.createEntityManager();
		RunLog rl = em.find(RunLog.class, launchId);
		
		File f = new File(rl.logPath);
		return f;
	}
	
	@GET
	@Path("/order/launch/outofplan/{appId}/{stateId}/{placeId}")
	@Produces("application/json")
	public ResOrder orderLaunchOutOfPlan(@PathParam("appId") UUID appId, @PathParam("stateId") UUID stateId, @PathParam("placeId") UUID placeId)
	{
		try
		{
			Application a = ctx.applicationsById.get(appId);
			Place p = a.getPlace(placeId);
			State s = a.getState(stateId);
			SenderHelpers.runStateAlone(s, p, ctx);
		}
		catch (Exception e)
		{
			return new ResOrder("LaunchOutOfPlan", false, e.getMessage());
		}
		return new ResOrder("LaunchOutOfPlan", true, "The order was sent successfuly");
	}
	
	@GET
	@Path("/order/launch/outofplan/duplicatelaunch/{launchId}")
	@Produces("application/json")
	public ResOrder orderLaunchOutOfPlan(@PathParam("launchId") UUID launchId)
	{
		EntityManager em = emfHistory.createEntityManager();
		RunLog rl = em.find(RunLog.class, launchId);
		return orderLaunchOutOfPlan(UUID.fromString(rl.applicationId), UUID.fromString(rl.stateId), UUID.fromString(rl.getPlaceId()));
	}
}
