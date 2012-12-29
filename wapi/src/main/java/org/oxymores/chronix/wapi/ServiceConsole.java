package org.oxymores.chronix.wapi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
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
			return rl.shortLog;
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

}
