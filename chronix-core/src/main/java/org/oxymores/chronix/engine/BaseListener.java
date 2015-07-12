package org.oxymores.chronix.engine;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.commons.lang.NotImplementedException;
import org.apache.log4j.Logger;
import org.oxymores.chronix.core.ChronixContext;

public class BaseListener implements MessageListener
{
    private static Logger log = Logger.getLogger(BaseListener.class);

    protected ChronixContext ctx;
    protected Broker broker;
    protected String brokerName;

    protected EntityManager emHistory, emTransac;
    protected EntityTransaction trHistory, trTransac;

    protected Session jmsSession;
    protected Connection jmsConnection;

    protected String qName;
    private List<Destination> qDestinations = new ArrayList<>();
    private List<MessageConsumer> qConsumers = new ArrayList<>();

    protected void init(Broker b, boolean initTransacContext, boolean initHistoryContext) throws JMSException
    {
        // Save pointers
        this.broker = b;
        this.jmsConnection = broker.getConnection();
        this.ctx = broker.getCtx();
        this.brokerName = broker.getBrokerName();

        // Persistence on two contexts
        if (initHistoryContext)
        {
            this.emHistory = this.ctx.getHistoryEM();
            this.trHistory = this.emHistory.getTransaction();
        }
        if (initTransacContext)
        {
            this.emTransac = this.ctx.getTransacEM();
            this.trTransac = this.emTransac.getTransaction();
        }

        // Open the JMS session that will be used for listening to messages
        this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
    }

    protected Queue subscribeTo(String qName) throws JMSException
    {
        log.debug(String.format("Broker %s: registering a message listener on queue %s", brokerName, qName));
        Queue qDestination = this.jmsSession.createQueue(qName);
        MessageConsumer qConsumer = this.jmsSession.createConsumer(qDestination);
        qConsumer.setMessageListener(this);

        this.qConsumers.add(qConsumer);
        this.qDestinations.add(qDestination);

        return qDestination;
    }

    protected void jmsCommit()
    {
        try
        {
            jmsSession.commit();
        }
        catch (JMSException e)
        {
            log.error("failure to commit an event consumption in JMS queue" + qName
                    + ". Scheduler will now abort as it is a dangerous situation. You may need to empty all queues before restarting.", e);
            broker.stop();
            return;
        }
    }

    protected void jmsRollback()
    {
        try
        {
            jmsSession.rollback();
        }
        catch (JMSException e)
        {
            log.error("Failed to rollback an event consumption in JMS queue " + qName
                    + ". Scheduler will now abort as it is a dangerous situation. You may need to empty the queue before restarting.", e);
            broker.stop();
            return;
        }
    }

    @Override
    public void onMessage(Message arg0)
    {
        throw new NotImplementedException();
    }

    void stopListening()
    {
        log.trace("Stop request received for thread " + this.getClass() + "(context " + this.ctx.getContextRoot() + ")");
        try
        {
            for (MessageConsumer mc : this.qConsumers)
            {
                mc.close();
            }
            this.jmsSession.close();
        }
        catch (JMSException e)
        {
            log.warn("An error occurred while closing a listener. Shutdown issues are just warnings, but please report this.", e);
        }
        if (emTransac != null)
        {
            try
            {
                emTransac.getTransaction().rollback();
            }
            catch (Exception e)
            {
                // Nothing to do - we just close the transaction if it's open
            }
            emTransac.close();
        }
        if (emHistory != null)
        {
            try
            {
                emHistory.getTransaction().rollback();
            }
            catch (Exception e)
            {
                // Nothing to do - we just close the transaction if it's open
            }
            emHistory.close();
        }
    }

}
