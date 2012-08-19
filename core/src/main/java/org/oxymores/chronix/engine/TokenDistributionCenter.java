package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
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
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.Token;
import org.oxymores.chronix.core.transactional.TokenReservation;
import org.oxymores.chronix.engine.TokenRequest.TokenRequestType;

public class TokenDistributionCenter extends Thread implements MessageListener {
	private static Logger log = Logger.getLogger(TokenDistributionCenter.class);

	private Session jmsSession;
	private Destination tokenQueueDestination;
	private Connection jmsConnection;
	private MessageProducer jmsProducer;
	private MessageConsumer jmsOrderConsumer;

	private EntityManager em;
	private EntityTransaction emTransac;

	private ChronixContext ctx;
	private String brokerName;

	private boolean running = true;
	private Semaphore mainLoop, localResource;

	private ArrayList<TokenReservation> shouldRenew;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx) throws JMSException {
		log.debug(String.format("(%s) Initializing TokenDistributionCenter", ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;
		this.brokerName = brokerName;
		this.em = this.ctx.getTransacEM();
		this.emTransac = this.em.getTransaction();

		// Sync
		mainLoop = new Semaphore(0);
		localResource = new Semaphore(1);

		this.shouldRenew = new ArrayList<TokenReservation>();

		// Register current object as a listener on ORDER queue
		String qName = String.format("Q.%s.TOKEN", brokerName);
		log.debug(String.format("Broker %s: registering a token listener on queue %s", this.brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsProducer = this.jmsSession.createProducer(null);
		this.tokenQueueDestination = this.jmsSession.createQueue(qName);
		this.jmsOrderConsumer = this.jmsSession.createConsumer(tokenQueueDestination);
		this.jmsOrderConsumer.setMessageListener(this);

		// Start thread
		this.start();
	}

	public void stopListening() throws Exception {
		running = false;
		mainLoop.release();
		localResource.acquire();
		this.jmsOrderConsumer.close(); // waits for the end of onMessage if necessary
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg) {
		ObjectMessage omsg = (ObjectMessage) msg;
		TokenRequest request;
		try {
			Object o = omsg.getObject();
			if (o instanceof TokenRequest) {
				request = (TokenRequest) o;
			} else {
				log.warn("An object was received on the token queue but was not a token request or token request ack! Ignored.");
				jmsSession.commit();
				return;
			}
		} catch (JMSException e) {
			log.error("An error occurred during token reception. BAD. Message will stay in queue and will be analysed later", e);
			try {
				jmsSession.rollback();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
			return;
		}

		// Check.
		Application a = null;
		try {
			a = ctx.applicationsById.get(request.applicationID);
		} catch (Exception e) {
		}
		if (a == null) {
			log.warn("A token for an application that does not run locally was received. Ignored.");
			try {
				jmsSession.commit();
			} catch (JMSException e) {
			}
			return;
		}

		Token tk = a.getToken(request.tokenID);
		Place p = a.getPlace(request.placeID);
		org.oxymores.chronix.core.State s = a.getState(request.stateID);
		ExecutionNode en = a.getNode(request.requestingNodeID);

		log.debug(String.format("Received a %s token request type %s on %s for state %s (application %s) for node %s. Local: %s", request.type,
				tk.getName(), p.getName(), s.getRepresents().getName(), a.getName(), en.getBrokerName(), request.local));

		// Case 1: TDC proxy: local request.
		if (request.local && request.type == TokenRequestType.REQUEST) {
			// Should be stored locally, so as to refresh the request every 5 minutes
			TokenReservation tr = new TokenReservation();
			tr.applicationId = request.applicationID.toString();
			tr.grantedOn = new Date();
			tr.localRenew = true;
			tr.placeId = request.placeID.toString();
			tr.renewedOn = tr.grantedOn;
			tr.requestedOn = tr.grantedOn;
			tr.stateId = request.stateID.toString();
			tr.tokenId = request.tokenID.toString();
			tr.pending = true;
			tr.requestedBy = request.requestingNodeID.toString();

			emTransac.begin();
			em.persist(tr);
			emTransac.commit();
			shouldRenew.add(tr);

			// Request should be sent to the node responsible for the distribution of this token
			request.local = false;
			try {
				SenderHelpers.sendTokenRequest(request, ctx, jmsSession, jmsProducer, false);
			} catch (JMSException e) {
				log.error(e.getMessage(), e);
				try {
					jmsSession.rollback();
				} catch (JMSException e1) {
				}
				return;
			}
			request.local = true;
		}

		// Case 2: TDC proxy: local release
		if (request.local && request.type == TokenRequestType.RELEASE) {
			// Delete the local element
			TokenReservation toRemove = null;
			for (TokenReservation tr : shouldRenew) {
				if (tr.stateId == request.stateID.toString() && tr.placeId == request.placeID.toString()) {
					toRemove = tr;
				}
			}
			if (toRemove != null) {
				shouldRenew.remove(toRemove);
			}

			// Request should be sent to the node responsible for the distribution of this token
			request.local = false;
			try {
				SenderHelpers.sendTokenRequest(request, ctx, jmsSession, jmsProducer, false);
			} catch (JMSException e) {
				log.error(e.getMessage(), e);
				try {
					jmsSession.rollback();
				} catch (JMSException e1) {
				}
				return;
			}
			request.local = true;
		}

		// Case 3: TDC: request
		if (!request.local && request.type == TokenRequestType.REQUEST) {
			emTransac.begin();
			log.debug(String.format("Analysing token request for PJ %s", request.pipelineJobID));
			processRequest(request);
			emTransac.commit();
		}

		// Case 4: TDC: release
		if (!request.local && request.type == TokenRequestType.RELEASE) {
			// Data

			// Log
			log.info(String.format("A token %s that was granted on %s (application %s) to node %s on state %s is released", tk.getName(),
					p.getName(), a.getName(), en.getBrokerName(), s.getRepresents().getName()));

			// Find the element
			TokenReservation tr = getTR(request);

			emTransac.begin();
			em.remove(tr);
			emTransac.commit();
		}

		// Case 5: TDC: RENEW
		if (!request.local && request.type == TokenRequestType.RENEW) {
			// Log
			log.debug(String.format("A token %s that was granted on %s (application %s) to node %s on state %s is renewed", tk.getName(),
					p.getName(), a.getName(), en.getBrokerName(), s.getRepresents().getName()));

			// Find the element
			TokenReservation tr = getTR(request);

			emTransac.begin();
			tr.renewedOn = new Date();
			emTransac.commit();
		}

		// Case 6: TDC proxy: AGREE
		if (!request.local && request.type == TokenRequestType.AGREE) {
			// Just forward it to the pipeline
			ObjectMessage response;
			try {
				response = jmsSession.createObjectMessage(request);
				String qName = String.format("Q.%s.PJ", a.getLocalNode().getHost().getBrokerName());
				Destination d = jmsSession.createQueue(qName);
				log.debug(String.format("A message will be sent to queue %s", qName));
				jmsProducer.send(d, response);
			} catch (JMSException e) {
				log.error(e.getMessage(), e);
				try {
					jmsSession.rollback();
				} catch (JMSException e1) {
				}
				return;
			}
		}

		// TDC: Step 1: Purge granted requests that are too old
		if (!request.local) {
			emTransac.begin();
			TypedQuery<TokenReservation> q = em.createQuery("SELECT q from TokenReservation q where q.renewedOn < ?1 AND q.pending = FALSE",
					TokenReservation.class);
			q.setParameter(1, DateTime.now().minusMinutes(10).toDate());
			for (TokenReservation tr : q.getResultList()) {
				Application aa = ctx.applicationsById.get(tr.applicationId);
				org.oxymores.chronix.core.State ss = aa.getState(UUID.fromString(tr.stateId));
				Place pp = aa.getPlace(UUID.fromString(tr.placeId));
				ExecutionNode enn = aa.getNode(UUID.fromString(tr.requestedBy));
				log.info(String
						.format("A token that was granted on %s (application %s) to node %s on state %s will be revoked as the request was not renewed in the last 10 minutes",
								pp.getName(), aa.getName(), enn.getBrokerName(), ss.getRepresents().getName()));

				// Remove from database
				em.remove(tr);
			}
		}

		// TDC: Step 2: Now that the purge is gone, analyse pending requests again - tokens may have freed
		if (!request.local) {
			TypedQuery<TokenReservation> q = em.createQuery("SELECT q from TokenReservation q where q.pending = TRUE AND q.localRenew = FALSE",
					TokenReservation.class);
			for (TokenReservation tr : q.getResultList()) {
				log.debug(String.format("Re-analysing token request for PJ %s", tr.pipelineJobId));
				processRequest(tr);
			}
			emTransac.commit();
		}

		// The end: commit JMS
		try {
			jmsSession.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("Commit successful");
	}

	private TokenReservation getTR(TokenRequest request) {
		TypedQuery<TokenReservation> q = em.createQuery("SELECT q from TokenReservation q where q.pipelineJobId = ?1 AND q.localRenew = FALSE",
				TokenReservation.class);
		q.setParameter(1, request.pipelineJobID.toString());

		TokenReservation tr = q.getSingleResult();
		return tr;
	}

	private void processRequest(TokenRequest request) {
		// Get data
		Application a = ctx.applicationsById.get(request.applicationID);
		Token tk = a.getToken(request.tokenID);
		Place p = a.getPlace(request.placeID);
		org.oxymores.chronix.core.State s = a.getState(request.stateID);

		// process
		processRequest(a, tk, p, request.requestedAt, s, null, request.pipelineJobID.toString(), request.requestingNodeID.toString());
	}

	private void processRequest(TokenReservation tr) {
		// Get data
		Application a = ctx.applicationsById.get(UUID.fromString(tr.applicationId));
		Token tk = a.getToken(UUID.fromString(tr.tokenId));
		Place p = a.getPlace(UUID.fromString(tr.placeId));
		org.oxymores.chronix.core.State s = a.getState(UUID.fromString(tr.stateId));

		// Process
		processRequest(a, tk, p, new DateTime(tr.requestedOn), s, tr, tr.pipelineJobId, tr.requestedBy);
	}

	private void processRequest(Application a, Token tk, Place p, DateTime requestedOn, org.oxymores.chronix.core.State s, TokenReservation existing,
			String pipelineJobId, String requestingNodeId) {

		// Locate all the currently allocated tokens on this Token/Place
		TypedQuery<TokenReservation> q = null;
		if (tk.isByPlace()) {
			q = em.createQuery(
					"SELECT q from TokenReservation q where q.tokenId = ?1 AND q.renewedOn > ?2 AND q.placeId = ?3 AND q.pending = FALSE AND q.localRenew = FALSE",
					TokenReservation.class);
			q.setParameter(1, tk.getId().toString());
			q.setParameter(2, DateTime.now().minusMinutes(5).toDate());
			q.setParameter(3, p.getId().toString());
		} else {
			q = em.createQuery(
					"SELECT q from TokenReservation q where q.tokenId = ?1 AND q.renewedOn > ?2 AND q.pending = FALSE AND q.localRenew = FALSE",
					TokenReservation.class);
			q.setParameter(1, tk.getId().toString());
			q.setParameter(2, DateTime.now().minusMinutes(5).toDate());
		}
		List<TokenReservation> res = q.getResultList();

		if (res.size() >= tk.getCount()) {
			// No available token
			log.warn(String.format("A token was requested but there are none available. (max is %s, allocated is %s)", tk.getCount(), res.size()));

			// Store the request if not done already
			if (existing == null) {
				TokenReservation trs = new TokenReservation();
				trs.grantedOn = null;
				trs.localRenew = false;
				trs.pending = true; // PENDING means not given yet
				trs.placeId = p.getId().toString();
				trs.renewedOn = null;
				trs.requestedOn = requestedOn.toDate();
				trs.stateId = s.getId().toString();
				trs.tokenId = tk.getId().toString();
				trs.pipelineJobId = pipelineJobId;
				trs.applicationId = a.getId().toString();
				trs.requestedBy = requestingNodeId;

				em.persist(trs);
			} else {
				existing.renewedOn = new Date();
			}

			// Don't send an answer to the caller.
		} else {
			// Available token
			log.debug(String.format("A token was requested and one can be issued (%s taken out of %s)", res.size(), tk.getCount()));

			TokenRequest answer = null;
			if (existing == null) {
				TokenReservation trs = new TokenReservation();
				trs.grantedOn = new Date();
				trs.localRenew = false;
				trs.pending = false;
				trs.placeId = p.getId().toString();
				trs.renewedOn = trs.grantedOn;
				trs.requestedOn = requestedOn.toDate();
				trs.stateId = s.getId().toString();
				trs.tokenId = tk.getId().toString();
				trs.pipelineJobId = pipelineJobId;
				trs.applicationId = a.getId().toString();
				trs.requestedBy = requestingNodeId;

				em.persist(trs);
				answer = trs.getAgreeRequest();
			} else {
				existing.pending = false;
				existing.grantedOn = new Date();
				existing.renewedOn = existing.grantedOn;
				answer = existing.getAgreeRequest();
			}

			// Send an answer
			ObjectMessage response;
			try {
				response = jmsSession.createObjectMessage(answer);
				Destination d = jmsSession.createQueue(String.format("Q.%s.TOKEN", p.getNode().getHost().getBrokerName()));
				jmsProducer.send(d, response);
			} catch (JMSException e) {
				log.error(e.getMessage(), e);
				try {
					jmsSession.rollback();
				} catch (JMSException e1) {
				}
				return;
			}

		}
	}

	@Override
	public void run() {
		do {
			DateTime now = DateTime.now();

			// Sync
			try {
				localResource.acquire();
			} catch (InterruptedException e1) {
			}

			// Renew token location for all boring
			for (TokenReservation tr : shouldRenew) {
				if (now.minusMinutes(5).compareTo(new DateTime(tr.renewedOn)) <= 0) {
					try {
						log.debug("Sending a renewal request for token state");
						SenderHelpers.sendTokenRequest(tr.getRenewalRequest(), ctx, jmsSession, jmsProducer, false);
					} catch (JMSException e) {
					}
				}
			}

			// Sync
			localResource.release();

			try {
				mainLoop.tryAcquire(60, TimeUnit.SECONDS); // Loop every minute or so. No need to be precise here.
			} catch (InterruptedException e) {
			}
		} while (running);
	}
}
