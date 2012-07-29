package org.oxymores.chronix.engine;

import java.util.ArrayList;
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
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;

public class TranscientListener implements MessageListener {
	private static Logger log = Logger.getLogger(TranscientListener.class);

	private Session jmsSession;
	private Destination logQueueDestination;
	private Connection jmsConnection;
	private EntityManager em;
	private EntityTransaction tr;
	private ChronixContext ctx;
	private MessageProducer producerEvent;

	public void startListening(Connection cnx, String brokerName,
			ChronixContext ctx, EntityManagerFactory emf) throws JMSException {
		log.debug(String.format("Initializing LogListener on context %s",
				ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;

		// Register current object as a listener on LOG queue
		String qName = String.format("Q.%s.CALENDARPOINTER", brokerName);
		log.debug(String.format(
				"Broker %s: registering a transcient listener on queue %s",
				brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true,
				Session.SESSION_TRANSACTED);
		this.logQueueDestination = this.jmsSession.createQueue(qName);
		MessageConsumer consumer = this.jmsSession
				.createConsumer(logQueueDestination);
		consumer.setMessageListener(this);

		// Producers
		qName = String.format("Q.%s.EVENT", brokerName);
		Destination d = this.jmsSession.createQueue(qName);
		producerEvent = this.jmsSession.createProducer(d);

		// Persistence on transac context
		em = emf.createEntityManager();
		tr = em.getTransaction();
	}

	@Override
	public void onMessage(Message msg) {
		// Read message (don't commit yet)
		ObjectMessage omsg = (ObjectMessage) msg;
		Object o = null;
		try {
			o = omsg.getObject();

		} catch (JMSException e) {
			log.error(
					"An error occurred during transcient reception. BAD. Message will stay in queue and will be analysed later",
					e);
			try {
				jmsSession.rollback();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
			return;
		}

		tr.begin();

		// ////////////////////////////////////////
		// CalendarPointer
		if (o instanceof CalendarPointer) {
			log.debug("A calendar pointer was received");
			CalendarPointer cp = (CalendarPointer) o;
			Calendar ca = cp.getCalendar(ctx);
			cp = em.merge(cp);

			// Re analyse events that may benefit from this calendar change
			// All events still there are supposed to be waiting for a new
			// analysis.
			List<State> states_using_calendar = ca.getUsedInStates();
			List<String> ids = new ArrayList<String>();
			for (State s : states_using_calendar) {
				// events come from states *before* the ones that use the
				// calendar
				for (Transition tr : s.getTrReceivedHere()) {
					ids.add(tr.getStateFrom().getId().toString());
				}
			}

			TypedQuery<Event> q = em.createQuery(
					"SELECT e from Event e WHERE e.stateID IN ( :ids )",
					Event.class);
			q.setParameter("ids", ids);
			List<Event> events = q.getResultList();

			// Send these events for analysis (local only - every execution node
			// has also received this pointer)
			log.info(String
					.format("The updated calendar pointer may have impacts on %s events that will have to be reanalysed",
							events.size()));
			for (Event e : events) {
				try {
					ObjectMessage om = jmsSession.createObjectMessage(e);
					producerEvent.send(om);
				} catch (JMSException e1) {
					log.error("Impossible to send an event locally... Will be attenpted next reboot");
					try {
						jmsSession.rollback();
					} catch (JMSException e2) {
					}
					return;
				}
			}
		}
		// end calendar pointers
		// /////////////////////////////////////////////////

		// End: commit both JPA and JMS
		tr.commit();
		try {
			jmsSession.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("Saved correctly");
	}
}
