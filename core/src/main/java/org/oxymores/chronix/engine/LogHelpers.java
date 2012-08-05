package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.TranscientBase;

public class LogHelpers {
	private static Logger log = Logger.getLogger(LogHelpers.class);

	public static List<RunLog> displayAllHistory() {
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

	public static void clearAllTranscientElements() {
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("HistoryUnit");
		EntityManager em1 = emf.createEntityManager();
		TypedQuery<RunLog> q = em1.createQuery("SELECT r FROM RunLog r",
				RunLog.class);

		EntityTransaction tr1 = em1.getTransaction();
		tr1.begin();
		for (RunLog l : q.getResultList())
			em1.remove(l);
		
		tr1.commit();

		emf = Persistence
				.createEntityManagerFactory("TransacUnit");
		EntityManager em2 = emf.createEntityManager();
		TypedQuery<TranscientBase> q2 = em2.createQuery("SELECT r FROM TranscientBase r",
				TranscientBase.class);
		
		EntityTransaction tr2 = em2.getTransaction();
		tr2.begin();
		for (TranscientBase b : q2.getResultList())
			em2.remove(b);
		tr2.commit();
	}
}
