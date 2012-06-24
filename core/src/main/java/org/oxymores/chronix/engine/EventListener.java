package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.transactional.Event;

public class EventListener implements MessageListener {

	private static Logger log = Logger.getLogger(EventListener.class);

	private ChronixContext ctx;
	private Session session;
	private Destination dest;
	private Connection cnx;
	EntityManagerFactory emf;
	EntityManager entityManager;
	private MessageProducer producerPJ;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		this.ctx = ctx;
		this.cnx = cnx;
		String qName = String.format("Q.%s.EVENT", brokerName);
		log.debug(String.format(
				"Broker %s: registering an event listener on queue %s",
				brokerName, qName));
		this.session = this.cnx.createSession(true, Session.SESSION_TRANSACTED);
		this.dest = this.session.createQueue(qName);
		MessageConsumer consumer = this.session.createConsumer(dest);
		consumer.setMessageListener(this);

		this.emf = emf;
		entityManager = this.emf.createEntityManager();

		qName = String.format("Q.%s.PJ", brokerName);
		producerPJ = session.createProducer(null);
	}

	@Override
	public void onMessage(Message msg) {

		// For commits: remember an event can be analyzed multiple times
		// without problems.
		ObjectMessage omsg = (ObjectMessage) msg;
		Event evt;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof Event)) {
				log.warn("An object was received on the event queue but was not an event! Ignored.");
				session.commit();
				return;
			}
			evt = (Event) o;
		} catch (JMSException e) {
			log.error(
					"An error occurred during event reception. BAD. Message will stay in queue and will be analysed later",
					e);
			rollback();
			return;
		}

		//
		// TODO: Analyse event!
		//
		EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();

		// Get data from event
		Application a = evt.getApplication(ctx);
		State s = evt.getState(ctx);
		ActiveNodeBase active = s.getRepresents();
		log.debug(String
				.format("Event %s (from application %s / active node %s) was received and will be analysed",
						evt.getId(), a.getName(), active.getName()));

		// Should it be discarded?
		if (evt.getBestBefore() != null
				&& evt.getBestBefore().before(new Date())) {
			log.info(String
					.format("Event %s (from application %s / active node %s) was discarded because it was too old according to its 'best before' date",
							evt.getId(), a.getName(), active.getName()));
			commit();
			return;
		}

		// All clients
		ArrayList<State> clientStates = s.getClientStates();

		// All client physical nodes
		ArrayList<ExecutionNode> clientPN = new ArrayList<ExecutionNode>();
		for (State st : clientStates) {
			for (ExecutionNode en : st.getRunsOnPhysicalNodes()) {
				if (!clientPN.contains(en))
					clientPN.add(en);
			}
		}

		// All local clients
		ArrayList<State> localConsumers = new ArrayList<State>();
		for (State st : clientStates) {
			if (st.getApplication().equals(a))
				localConsumers.add(st);
		}

		// Analyze on every local consumer
		EventAnalysisResult res = new EventAnalysisResult();
		for (State st : localConsumers) {
			res.add(st.getRepresents().isStateExecutionAllowed(st, evt,
					entityManager, producerPJ, session, ctx));
		}

		// if ()

		// Ack
		log.debug(String
				.format("Event id %s was received, analysed and will now be acked in the JMS queue",
						evt.getId()));
		entityManager.persist(evt);
		transaction.commit();
		commit();
		log.debug(String.format(
				"Event id %s was received, analysed and acked all right",
				evt.getId()));

		// Purge
		transaction.begin();
		this.cleanUp(res.consumedEvents, entityManager);
		transaction.commit();
	}

	private void commit() {
		try {
			session.commit();
		} catch (JMSException e) {
			log.error(
					"failure to acknowledge an event consumption in the JMS queue. Scheduler will now abort as it is a dangerous situation. Empty the EVENT queue before restarting.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void rollback() {
		try {
			session.rollback();
		} catch (JMSException e) {
			log.error(
					"failure to rollback an event consumption in the JMS queue. Scheduler will now abort as it is a dangerous situation. Empty the EVENT queue before restarting.",
					e);
			// TODO: stop the engine. Well, as soon as we HAVE an engine to
			// stop.
			return;
		}
	}

	private void cleanUp(List<Event> events, EntityManager em) {
		for (Event e : events) {
			State s = e.getState(ctx);
			ArrayList<State> clientStates = s.getClientStates();

			for (State cs : clientStates) {
				for (Place p : cs.getRunsOn().getPlaces()) {
					if (e.wasConsumedOnPlace(p, cs)) {
						em.remove(e);
						log.debug(String.format("Event %s will be purged",
								e.getId()));
					}
				}
			}
		}
	}
}
