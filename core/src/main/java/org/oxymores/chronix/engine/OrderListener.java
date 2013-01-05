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

import java.util.Date;
import java.util.UUID;

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

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.External;
import org.oxymores.chronix.core.timedata.RunLog;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class OrderListener implements MessageListener
{
	private static Logger log = Logger.getLogger(OrderListener.class);

	private Session jmsSession;
	private Destination orderQueueDestination;
	private Connection jmsConnection;
	private MessageProducer jmsProducer;
	private MessageConsumer jmsOrderConsumer;
	private EntityManager emTransac, emHistory;
	private ChronixContext ctx;
	private String brokerName;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx) throws JMSException
	{
		log.debug(String.format("(%s) Initializing OrderListener", ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;
		this.brokerName = brokerName;
		this.emTransac = this.ctx.getTransacEM();
		this.emHistory = this.ctx.getHistoryEM();

		// Register current object as a listener on ORDER queue
		String qName = String.format("Q.%s.ORDER", brokerName);
		log.debug(String.format("Broker %s: registering an order listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsProducer = this.jmsSession.createProducer(null);
		this.orderQueueDestination = this.jmsSession.createQueue(qName);
		this.jmsOrderConsumer = this.jmsSession.createConsumer(orderQueueDestination);
		this.jmsOrderConsumer.setMessageListener(this);
	}

	public void stopListening() throws JMSException
	{
		this.jmsOrderConsumer.close();
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg)
	{
		ObjectMessage omsg = (ObjectMessage) msg;
		Order order;
		try
		{
			Object o = omsg.getObject();
			if (!(o instanceof Order))
			{
				log.warn("An object was received on the order queue but was not an order! Ignored.");
				jmsSession.commit();
				return;
			}
			order = (Order) o;
		} catch (JMSException e)
		{
			log.error("An error occurred during order reception. BAD. Message will stay in queue and will be analysed later", e);
			try
			{
				jmsSession.rollback();
			} catch (JMSException e1)
			{
				e1.printStackTrace();
			}
			return;
		}

		log.info(String.format("An order was received. Type: %s", order.type));

		if (order.type == OrderType.RESTARTPJ)
		{
			// Find the PipelineJob
			PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);

			// Put the pipeline job inside the local pipeline
			try
			{
				String qName = String.format("Q.%s.PJ", brokerName);
				log.info(String.format("A relaunch PJ will be sent for execution on queue %s", qName));
				Destination destination = this.jmsSession.createQueue(qName);
				ObjectMessage m = jmsSession.createObjectMessage(pj);
				jmsProducer.send(destination, m);
			} catch (Exception e)
			{
				log.error("An error occurred while processing a relaunch order. The order will be ignored", e);
			}
		}

		if (order.type == OrderType.FORCEOK)
		{
			// Find the PipelineJob
			PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);
			if (pj != null && pj.getStatus().equals("DONE"))
			{
				try
				{
					ActiveNodeBase a = pj.getActive(ctx);
					RunResult rr = a.forceOK();
					Event e = pj.createEvent(rr);
					SenderHelpers.sendEvent(e, jmsProducer, jmsSession, ctx, false);

					// Update history & PJ
					EntityTransaction t1 = emTransac.getTransaction();
					EntityTransaction t2 = emHistory.getTransaction();
					t1.begin();
					t2.begin();
					pj.setStatus("OVERRIDEN");
					RunLog rl = emHistory.find(RunLog.class, (String) order.data);
					if (rl != null)
					{
						rl.setLastKnownStatus("OVERRIDEN");
						rl.setLastLocallyModified(new Date());
					}
					t1.commit();
					t2.commit();
				} catch (Exception e)
				{
					log.error("An error occurred while processing a force OK order. The order will be ignored", e);
				}
			}
			else
			{
				if (pj == null)
					log.error("Job does not exist - cannot be forced!");
				else
					log.error("The job cannot be forced: it is not in state DONE (current state: " + pj.getStatus() + ")");
			}
		}

		if (order.type == OrderType.EXTERNAL)
		{
			// Find the external node
			State s = null;
			for (Application a : ctx.applicationsByName.values())
			{
				s = a.getState(((UUID) order.data));
				if (s != null)
					break;
			}
			if (s == null || !(s.getRepresents() instanceof External))
			{
				log.error(String.format(
						"An order of type EXTERNAL was received but its data was invalid %s. Corresponding State found was %s.",
						order.data, s));
				commit(); // destroy message - it's corrupt
				return;
			}

			External e = (External) s.getRepresents();
			String d = null;
			if (order.data2 != null)
				d = e.getCalendarString((String) order.data2);
			log.debug(String.format("Pattern  is %s - String is %s - Result is %s", e.getRegularExpression(), order.data2, d));

			Event evt = new Event();
			evt.setApplication(s.getApplication());
			if (d != null && s.getCalendar() != null)
			{
				evt.setCalendar(s.getCalendar());
				evt.setCalendarOccurrenceID(s.getCalendar().getOccurrence(d).getId().toString());
			}
			log.debug("rrrrrrrrrr" + evt.getCalendarOccurrenceID());
			evt.setConditionData1(0);
			evt.setLevel1IdU(new UUID(0, 1));
			evt.setLevel0IdU(s.getChain().getId());
			evt.setState(s);

			for (Place p : s.getRunsOn().getPlaces())
			{
				evt.setPlace(p);
				try
				{
					log.debug("Sending event for external source");
					SenderHelpers.sendEvent(evt, this.jmsProducer, this.jmsSession, this.ctx, false);
				} catch (JMSException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		commit();
	}

	private void commit()
	{
		try
		{
			jmsSession.commit();
		} catch (JMSException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
