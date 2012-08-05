package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.timedata.RunLog;

public class LogHelpers {
	private static Logger log = Logger.getLogger(LogHelpers.class);
	
	public static List<RunLog> displayAllHistory()
	{
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("HistoryUnit");
		EntityManager em = emf.createEntityManager();
		TypedQuery<RunLog> q = em
				.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt",
						RunLog.class);
		List<RunLog> res = q.getResultList();

		log.info(RunLog.getTitle());
		for (RunLog l : res) {
			log.info(l.getLine());
		}
		
		return res;
	}
}
