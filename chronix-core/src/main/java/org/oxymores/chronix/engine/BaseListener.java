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

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.oxymores.chronix.core.ChronixContext;
import org.slf4j.LoggerFactory;

class BaseListener implements MessageListener
{
    private static final Logger log = LoggerFactory.getLogger(BaseListener.class);

    protected ChronixContext ctx;
    protected Broker broker;
    protected String brokerName;

    protected Session jmsSession;
    protected Connection jmsConnection;

    protected String qName;
    private final List<Destination> qDestinations = new ArrayList<>();
    private final List<MessageConsumer> qConsumers = new ArrayList<>();

    protected void init(Broker b) throws JMSException
    {
        // Save pointers
        this.broker = b;
        this.jmsConnection = broker.getConnection();
        this.ctx = broker.getCtx();
        this.brokerName = broker.getBrokerName();

        // Open the JMS session that will be used for listening to messages
        this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
    }

    protected Queue subscribeTo(String qName) throws JMSException
    {
        log.debug(String.format("Registering a message listener on queue %s", qName));
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
    }

}
