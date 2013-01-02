/**
 * By Marc-Antoine Gouillart, 2012
 * 
 * See the NOTICE file distributed with this work for 
 * information regarding copyright ownership.
 * This file is licensed to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file 
 * except in compliance with the License. You may obtain 
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oxymores.chronix.engine;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.TranscientBase;

public class LogHelpers
{
	private static Logger log = Logger.getLogger(LogHelpers.class);

	public static List<RunLog> displayAllHistory(ChronixContext ctx)
	{
		EntityManager em = ctx.getHistoryEM();
		TypedQuery<RunLog> q = em.createQuery("SELECT r FROM RunLog r ORDER BY r.enteredPipeAt", RunLog.class);
		List<RunLog> res = q.getResultList();

		log.info(RunLog.getTitle());
		for (RunLog l : res)
		{
			log.info(l.getLine());
		}

		return res;
	}

	public static void clearAllTranscientElements(ChronixContext ctx)
	{
		try
		{
			// Clean history db
			EntityManager em1 = ctx.getHistoryEM();
			TypedQuery<RunLog> q = em1.createQuery("SELECT r FROM RunLog r", RunLog.class);

			EntityTransaction tr1 = em1.getTransaction();
			tr1.begin();
			for (RunLog l : q.getResultList())
				em1.remove(l);

			tr1.commit();

			// Clean OP db
			EntityManager em2 = ctx.getTransacEM();
			TypedQuery<TranscientBase> q2 = em2.createQuery("SELECT r FROM TranscientBase r", TranscientBase.class);

			EntityTransaction tr2 = em2.getTransaction();
			tr2.begin();
			for (TranscientBase b : q2.getResultList())
				em2.remove(b);

			em2.createQuery("DELETE FROM TokenReservation r").executeUpdate();
			em2.createQuery("DELETE FROM ClockTick r").executeUpdate();
			tr2.commit();
		} catch (Exception e)
		{
			log.warn(e.getMessage(), e);
		}
	}
}
