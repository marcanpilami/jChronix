package org.oxymores.chronix.engine;

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

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;

public class OrderListener implements MessageListener {
	private static Logger log = Logger.getLogger(OrderListener.class);

	private Session jmsSession;
	private Destination orderQueueDestination;
	private Connection jmsConnection;
	private MessageProducer jmsProducer;
	private MessageConsumer jmsOrderConsumer;
	private EntityManager emTransac;
	private ChronixContext ctx;
	private String brokerName;

	public void startListening(Connection cnx, String brokerName, ChronixContext ctx) throws JMSException {
		log.debug(String.format("(%s) Initializing OrderListener", ctx.configurationDirectory));

		// Save pointers
		this.jmsConnection = cnx;
		this.ctx = ctx;
		this.brokerName = brokerName;
		this.emTransac = this.ctx.getTransacEM();

		// Register current object as a listener on ORDER queue
		String qName = String.format("Q.%s.ORDER", brokerName);
		log.debug(String.format("Broker %s: registering an order listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsProducer = this.jmsSession.createProducer(null);
		this.orderQueueDestination = this.jmsSession.createQueue(qName);
		this.jmsOrderConsumer = this.jmsSession.createConsumer(orderQueueDestination);
		this.jmsOrderConsumer.setMessageListener(this);
	}

	public void stopListening() throws JMSException {
		this.jmsOrderConsumer.close();
		this.jmsSession.close();
	}

	@Override
	public void onMessage(Message msg) {
		ObjectMessage omsg = (ObjectMessage) msg;
		Order order;
		try {
			Object o = omsg.getObject();
			if (!(o instanceof Order)) {
				log.warn("An object was received on the order queue but was not an order! Ignored.");
				jmsSession.commit();
				return;
			}
			order = (Order) o;
		} catch (JMSException e) {
			log.error("An error occurred during order reception. BAD. Message will stay in queue and will be analysed later", e);
			try {
				jmsSession.rollback();
			} catch (JMSException e1) {
				e1.printStackTrace();
			}
			return;
		}

		log.info(String.format("An order was received. Type: %s", order.type));

		if (order.type == OrderType.RESTARTPJ) {
			// Find the PipelineJob
			PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);

			// Put the pipeline job inside the local pipeline
			try {
				String qName = String.format("Q.%s.PJ", brokerName);
				log.info(String.format("A relaunch PJ will be sent for execution on queue %s", qName));
				Destination destination = this.jmsSession.createQueue(qName);
				ObjectMessage m = jmsSession.createObjectMessage(pj);
				jmsProducer.send(destination, m);
			} catch (Exception e) {
				log.error("An error occurred while processing a relaunch order. The order will be ignored", e);
			}
		}

		if (order.type == OrderType.FORCEOK) {
			// Find the PipelineJob
			PipelineJob pj = emTransac.find(PipelineJob.class, (String) order.data);

			try {
				ActiveNodeBase a = pj.getActive(ctx);
				RunResult rr = a.forceOK();
				Event e = pj.createEvent(rr);
				SenderHelpers.sendEvent(e, jmsProducer, jmsSession, ctx, false);
			} catch (Exception e) {
				log.error("An error occurred while processing a force OK order. The order will be ignored", e);
			}
		}

		try {
			jmsSession.commit();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
