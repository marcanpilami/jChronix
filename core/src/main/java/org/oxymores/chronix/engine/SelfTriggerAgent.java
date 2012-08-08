package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;

public class SelfTriggerAgent extends Thread {
	private static Logger log = Logger.getLogger(SelfTriggerAgent.class);

	private Semaphore loop;
	private boolean run = true;
	private ArrayList<ActiveNodeBase> nodes;
	private EntityManager em;
	private ChronixContext ctx;
	private MessageProducer producerEvents;
	private Session jmsSession;
	private EntityTransaction jpaTransaction;

	public void stopAgent() {
		run = false;
		loop.release();
	}

	public void startAgent(EntityManagerFactory emf, ChronixContext ctx,
			Connection cnx) throws JMSException {
		log.debug(String.format("(%s) Agent responsible for clocks will start",
				ctx.configurationDirectoryPath));
		// Save pointers
		this.loop = new Semaphore(0);
		this.em = emf.createEntityManager();
		this.ctx = ctx;
		this.jmsSession = cnx.createSession(true, Session.SESSION_TRANSACTED);
		this.producerEvents = jmsSession.createProducer(null);
		this.jpaTransaction = this.em.getTransaction();

		// Get all self triggered nodes
		this.nodes = new ArrayList<ActiveNodeBase>();
		for (Application a : this.ctx.applicationsById.values()) {
			for (ActiveNodeBase n : a.getActiveElements().values()) {
				if (n.selfTriggered())
					this.nodes.add(n);
			}// TODO: select only clocks with local consequences
		}

		// Start thread
		this.start();
	}

	@Override
	public void run() {
		DateTime nextLoopTime = DateTime.now();
		long sToWait = 0;

		while (run) {
			// Wait for the required number of seconds
			try {
				loop.tryAcquire(sToWait, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				return;
			}
			if (!run)
				break; // Don't bother doing the final loop

			// Log
			log.debug("Self trigger agent loops");

			// Init the next loop time at a huge value
			DateTime now = DateTime.now();
			nextLoopTime = now.plusDays(1).minusMillis(now.getMillisOfDay());

			// Loop through all the self triggered nodes and get their next loop
			// time
			jpaTransaction.begin();
			DateTime tmp = null;
			for (ActiveNodeBase n : this.nodes) {
				try {
					tmp = n.selfTrigger(producerEvents, jmsSession, ctx, em);
				} catch (Exception e) {
					log.error("Error triggering clocks and the like", e);
				}
				if (tmp.compareTo(nextLoopTime) < 0)
					nextLoopTime = tmp;
			}

			// Commit
			try {
				jmsSession.commit();
			} catch (JMSException e) {
				log.error("Oups", e);
				return;
			}
			jpaTransaction.commit();

			sToWait = Seconds.secondsBetween(DateTime.now(), nextLoopTime)
					.getSeconds();
			log.debug(String
					.format("Self trigger agent will loop again in %s seconds",
							sToWait));
		}
	}
}
