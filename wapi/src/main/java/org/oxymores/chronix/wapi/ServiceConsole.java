package org.oxymores.chronix.wapi;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.internalapi.IServiceConsole;

public class ServiceConsole implements IServiceConsole
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
	public List<RunLog> getLog(Date from, Date to)
	{
		log.debug("Service getLog was called");
		EntityManager em = emfHistory.createEntityManager();
		TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
		List<RunLog> res = q.getResultList();
		log.debug("End of call to getLog - returning " + res.size());
		return res;
	}

}
