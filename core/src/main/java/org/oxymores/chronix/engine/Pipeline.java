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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.TokenRequest.TokenRequestType;

public class Pipeline extends Thread implements MessageListener
{
	private static Logger log = Logger.getLogger(Pipeline.class);

	private ChronixContext ctx;
	private Session jmsSession;
	private Destination jmsPipelineQueue;
	private Connection jmsConnection;
	private MessageConsumer jmsPipelineConsumer;
	private MessageProducer jmsJRProducer;

	EntityManagerFactory emf;
	EntityManager em_mainloop, em_injector;
	EntityTransaction transac_mainloop, transac_injector;

	private LinkedBlockingQueue<PipelineJob> entering;
	private ArrayList<PipelineJob> waiting_sequence;
	private ArrayList<PipelineJob> waiting_token;
	private ArrayList<PipelineJob> waiting_token_answer;
	private ArrayList<PipelineJob> waiting_exclusion;
	private ArrayList<PipelineJob> waiting_run;

	private Boolean run = true;
	private Semaphore analyze;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx, EntityManagerFactory emf) throws JMSException
	{
		// Pointers
		this.ctx = ctx;
		this.jmsConnection = cnx;
		this.analyze = new Semaphore(1);

		// Register on incoming queue
		String qName = String.format("Q.%s.PJ", brokerName);
		log.debug(String.format("(%s) Registering a pipeline listener on queue %s", ctx.configurationDirectoryPath, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsPipelineQueue = this.jmsSession.createQueue(qName);
		this.jmsPipelineConsumer = this.jmsSession.createConsumer(this.jmsPipelineQueue);
		this.jmsPipelineConsumer.setMessageListener(this);

		// Outgoing producer for job runner
		this.jmsJRProducer = this.jmsSession.createProducer(null);

		// OpenJPA stuff
		this.emf = emf;
		em_mainloop = emf.createEntityManager();
		transac_mainloop = em_mainloop.getTransaction();

		em_injector = emf.createEntityManager();
		transac_injector = em_injector.getTransaction();

		// Create analysis queues
		entering = new LinkedBlockingQueue<PipelineJob>();
		waiting_sequence = new ArrayList<PipelineJob>();
		waiting_token = new ArrayList<PipelineJob>();
		waiting_exclusion = new ArrayList<PipelineJob>();
		waiting_run = new ArrayList<PipelineJob>();
		waiting_token_answer = new ArrayList<PipelineJob>();

		// Retrieve jobs from previous service launches
		Query q = em_mainloop.createQuery("SELECT j FROM PipelineJob j WHERE j.status = ?1");
		q.setParameter(1, "CHECK_SYNC_CONDS");
		@SuppressWarnings("unchecked")
		List<PipelineJob> sessionEvents = q.getResultList();
		entering.addAll(sessionEvents);

		// Start thread
		this.start();
	}

	public void stopListening() throws JMSException
	{
		try
		{
			this.analyze.acquire(); // wait for end of analysis
		} catch (InterruptedException e)
		{
			// Do nothing
		}
		this.run = false;
		this.jmsJRProducer.close();
		this.jmsPipelineConsumer.close();
		this.jmsSession.close();
		this.analyze.release();
	}

	@Override
	public void run()
	{
		while (this.run)
		{
			// Poll for a job
			try
			{
				PipelineJob pj = entering.poll(1, TimeUnit.MINUTES);
				if (pj != null && pj.getAppID() != null)
				{
					Place p = null;
					org.oxymores.chronix.core.State s = null;
					Application a = null;
					try
					{
						a = pj.getApplication(ctx);
						p = pj.getPlace(ctx);
						s = pj.getState(ctx);
					} catch (Exception e)
					{
					}
					if (s == null || p == null || a == null)
					{
						log.error("A job was received in the pipeline without any corresponding local application data - ignored");
					}
					else
					{
						waiting_sequence.add(pj);
						log.debug(String.format("A job was registered in the pipeline: %s", pj.getId()));
					}
				}
			} catch (InterruptedException e)
			{
				// Interruption is all right - we want to loop from time to time
			}

			// Synchro - helps to stop properly
			try
			{
				this.analyze.acquire();
			} catch (InterruptedException e1)
			{
				// Do nothing
			}
			if (!this.run)
				break;

			// On to analysis
			ArrayList<PipelineJob> toAnalyse = new ArrayList<PipelineJob>();

			toAnalyse.clear();
			toAnalyse.addAll(waiting_sequence);
			for (PipelineJob j : toAnalyse)
			{
				anSequence(j);
			}

			toAnalyse.clear();
			toAnalyse.addAll(waiting_token);
			for (PipelineJob j : toAnalyse)
			{
				anToken(j);
			}

			toAnalyse.clear();
			toAnalyse.addAll(waiting_exclusion);
			for (PipelineJob j : toAnalyse)
			{
				anExclusion(j);
			}

			toAnalyse.clear();
			toAnalyse.addAll(waiting_run);
			for (PipelineJob j : toAnalyse)
			{
				runPJ(j);
			}
			this.analyze.release();
		}
	}

	@Override
	public void onMessage(Message msg)
	{
		ObjectMessage omsg = (ObjectMessage) msg;
		PipelineJob pj = null;
		TokenRequest rq = null;
		try
		{
			Object o = omsg.getObject();
			if (o instanceof PipelineJob)
			{
				pj = (PipelineJob) o;
			}
			else if (o instanceof TokenRequest)
			{
				rq = (TokenRequest) o;
			}
			else
			{
				log.warn("An object was received on the pipeline queue but was not a job or a token request answer! Ignored.");
				commit();
				return;
			}

		} catch (JMSException e)
		{
			log.error("An error occurred during job reception. BAD. Message will stay in queue and will be analysed later", e);
			rollback();
			return;
		}

		if (pj != null)
		{
			transac_injector.begin();

			pj.setStatus("CHECK_SYNC_CONDS"); // So that we find it again after a crash/stop
			pj.setEnteredPipeAt(DateTime.now().toDate());
			em_injector.merge(pj);

			transac_injector.commit();

			log.debug(String.format("A job has entered the pipeline: %s", pj.getId()));
			commit(); // JMS
			entering.add(pj);
		}

		if (rq != null)
		{
			log.debug("Token message received");
			try
			{
				this.analyze.acquire();
			} catch (InterruptedException e1)
			{
			}

			pj = null;
			for (PipelineJob jj : waiting_token_answer)
			{
				if (jj.getIdU().equals(rq.pipelineJobID))
					pj = jj;
			}
			if (pj == null)
			{
				log.warn("A token was given to an unexisting PJ. Ignored");
				this.analyze.release();
				commit();
				return;
			}

			waiting_token_answer.remove(pj);
			waiting_exclusion.add(pj);
			log.debug(String.format("Job %s has finished token analysis - on to exclusion analysis", pj.getId()));

			this.analyze.release();
			commit(); // JMS
			entering.add(new PipelineJob());
		}

	}

	private void commit()
	{
		try
		{
			jmsSession.commit();
		} catch (JMSException e)
		{
			log.error("failure to acknowledge a message in the JMS queue. Scheduler will now abort as it is a dangerous situation.", e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void rollback()
	{
		try
		{
			jmsSession.rollback();
		} catch (JMSException e)
		{
			log.error("failure to rollback an message in the JMS queue. Scheduler will now abort as it is a dangerous situation.", e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void anSequence(PipelineJob pj)
	{
		// TODO: really check sequences
		waiting_sequence.remove(pj);
		waiting_token.add(pj);
		log.debug(String.format("Job %s has finished sequence analysis - on to token analysis", pj.getId()));
	}

	private void anToken(PipelineJob pj)
	{
		org.oxymores.chronix.core.State s = pj.getState(ctx);
		if (s.getTokens().size() > 0)
		{
			for (Token tk : s.getTokens())
			{
				TokenRequest tr = new TokenRequest();
				tr.applicationID = UUID.fromString(pj.getAppID());
				tr.local = true;
				tr.placeID = UUID.fromString(pj.getPlaceID());
				tr.requestedAt = new DateTime();
				tr.requestingNodeID = pj.getApplication(ctx).getLocalNode().getHost().getId();
				tr.stateID = pj.getStateIDU();
				tr.tokenID = tk.getId();
				tr.type = TokenRequestType.REQUEST;
				tr.pipelineJobID = pj.getIdU();

				try
				{
					SenderHelpers.sendTokenRequest(tr, ctx, jmsSession, jmsJRProducer, true);
				} catch (JMSException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				waiting_token.remove(pj);
				waiting_token_answer.add(pj);
			}
		}
		else
		{
			// No tokens to analyse - continue advancing in the pipeline
			waiting_token.remove(pj);
			waiting_exclusion.add(pj);
			log.debug(String.format("Job %s has finished token analysis - on to exclusion analysis", pj.getId()));
		}
	}

	private void anExclusion(PipelineJob pj)
	{
		// TODO: really check exclusions
		waiting_exclusion.remove(pj);
		waiting_run.add(pj);
		log.debug(String.format("Job %s has finished exclusion analysis - on to run", pj.getId()));
	}

	private void runPJ(PipelineJob pj)
	{
		// Remove from queue
		waiting_run.remove(pj);

		transac_mainloop.begin(); // JPA transaction

		String qName = String.format("Q.%s.RUNNERMGR", pj.getPlace(ctx).getNode().getHost().getBrokerName());
		try
		{
			Destination d = jmsSession.createQueue(qName);
			ObjectMessage om = jmsSession.createObjectMessage(pj);
			jmsJRProducer.send(d, om);
		} catch (JMSException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		pj.setStatus("RUNNING");
		pj.setMarkedForRunAt(new Date());

		transac_mainloop.commit(); // JPA
		commit(); // JMS

		log.debug(String.format("Job %s was given to the runner queue %s", pj.getId(), qName));
	}
}
