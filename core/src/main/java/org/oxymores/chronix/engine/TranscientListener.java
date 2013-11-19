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
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;

class TranscientListener implements MessageListener
{
	private static Logger log = Logger.getLogger(TranscientListener.class);

	private Session jmsSession;
	private Destination logQueueDestination;
	private Connection jmsConnection;
	private MessageProducer producerEvent;
	private MessageConsumer jmsTranscientConsumer;

	private EntityManager em;
	private EntityTransaction tr;
	private ChronixContext ctx;

	void startListening(Connection cnx, String brokerName, ChronixContext ctx, EntityManagerFactory emf) throws JMSException
	{
		log.debug(String.format("(%s) Initializing LogListener", ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;

		// Register current object as a listener on LOG queue
		String qName = String.format("Q.%s.CALENDARPOINTER", brokerName);
		log.debug(String.format("Broker %s: registering a transcient listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.logQueueDestination = this.jmsSession.createQueue(qName);
		this.jmsTranscientConsumer = this.jmsSession.createConsumer(this.logQueueDestination);
		this.jmsTranscientConsumer.setMessageListener(this);

		// Producers
		qName = String.format("Q.%s.EVENT", brokerName);
		Destination d = this.jmsSession.createQueue(qName);
		this.producerEvent = this.jmsSession.createProducer(d);

		// Persistence on transac context
		this.em = emf.createEntityManager();
		this.tr = this.em.getTransaction();
	}

	void stopListening() throws JMSException
	{
		this.jmsTranscientConsumer.close();
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg)
	{
		// Read message (don't commit yet)
		ObjectMessage omsg = (ObjectMessage) msg;
		Object o = null;
		try
		{
			o = omsg.getObject();

		} catch (JMSException e)
		{
			log.error("An error occurred during transcient reception. BAD. Message will stay in queue and will be analysed later", e);
			try
			{
				jmsSession.rollback();
			} catch (JMSException e1)
			{
				e1.printStackTrace();
			}
			return;
		}

		tr.begin();

		// ////////////////////////////////////////
		// CalendarPointer
		if (o instanceof CalendarPointer)
		{
			log.debug("A calendar pointer was received");
			CalendarPointer cp = (CalendarPointer) o;
			Calendar ca = null;
			State ss = null;
			@SuppressWarnings("unused")
			Place pp = null;
			try
			{
				ca = cp.getCalendar(ctx);
				if (cp.getPlaceID() != null)
				{
					ss = cp.getState(ctx);
					pp = cp.getPlace(ctx);
				}
			} catch (Exception e)
			{
				log.error("A calendar pointer was received that was unrelated to the locally known plan - it was ignored");
				tr.rollback();
				try
				{
					jmsSession.commit();
				} catch (JMSException e1)
				{
				}
				return;
			}

			// Save it/update it. Beware, id are not the same throughout the
			// network, so query the logical key
			if (cp.getPlaceID() != null)
			{
				TypedQuery<CalendarPointer> q1 = em.createQuery(
						"SELECT cp FROM CalendarPointer cp WHERE cp.stateID=?1 AND cp.placeID=?2 AND cp.calendarID=?3",
						CalendarPointer.class);
				q1.setParameter(1, ss.getId().toString());
				q1.setParameter(2, cp.getPlaceID());
				q1.setParameter(3, cp.getCalendarID());
				CalendarPointer tmp = q1.getSingleResult();
				if (tmp != null)
					cp.setId(tmp.getId());
			}
			else
			{
				TypedQuery<CalendarPointer> q1 = em.createQuery(
						"SELECT cp FROM CalendarPointer cp WHERE cp.stateID IS NULL AND cp.placeID IS NULL AND cp.calendarID=?1",
						CalendarPointer.class);
				q1.setParameter(1, cp.getCalendarID());
				CalendarPointer tmp = q1.getSingleResult();
				if (tmp != null)
					cp.setId(tmp.getId());
			}

			cp = em.merge(cp);

			// Log
			String represents = "a calendar";
			if (cp.getPlaceID() != null)
				represents = ss.getRepresents().getName();
			log.debug(String.format(
					"The calendar pointer is now [Next run %s] [Previous OK run %s] [Previous run %s] [Latest started %s] on [%s]", cp
							.getNextRunOccurrenceCd(ctx).getValue(), cp.getLastEndedOkOccurrenceCd(ctx).getValue(), cp
							.getLastEndedOccurrenceCd(ctx).getValue(), cp.getLastStartedOccurrenceCd(ctx).getValue(), represents));

			// Re analyse events that may benefit from this calendar change
			// All events still there are supposed to be waiting for a new analysis.
			List<State> states_using_calendar = ca.getUsedInStates();
			List<String> ids = new ArrayList<String>();
			for (State s : states_using_calendar)
			{
				// Events come from states *before* the ones that use the calendar
				for (Transition tr : s.getTrReceivedHere())
				{
					ids.add(tr.getStateFrom().getId().toString());
				}
			}

			TypedQuery<Event> q = em.createQuery("SELECT e from Event e WHERE e.stateID IN ( :ids )", Event.class);
			q.setParameter("ids", ids);
			List<Event> events = q.getResultList();

			// Send these events for analysis (local only - every execution node
			// has also received this pointer)
			log.info(String.format("The updated calendar pointer may have impacts on %s events that will have to be reanalysed",
					events.size()));
			for (Event e : events)
			{
				try
				{
					ObjectMessage om = jmsSession.createObjectMessage(e);
					producerEvent.send(om);
				} catch (JMSException e1)
				{
					log.error("Impossible to send an event locally... Will be attempted next reboot");
					try
					{
						jmsSession.rollback();
					} catch (JMSException e2)
					{
					}
					return;
				}
			}

			// Some jobs may now be late (or later than before). Signal them.
			try
			{
				ca.processStragglers(em);
			} catch (Exception e1)
			{
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		// end calendar pointers
		// /////////////////////////////////////////////////

		// End: commit both JPA and JMS
		tr.commit();
		try
		{
			jmsSession.commit();
		} catch (JMSException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug("Saved correctly");
	}
}
