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
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.MDC;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The container for listener objects.
 */
class ListenerHolder implements MessageListener
{
    private static final Logger log = LoggerFactory.getLogger(ListenerHolder.class);

    protected Session jmsSession;
    protected Connection jmsConnection;
    protected MessageProducer jmsProducer; // Helper sender.
    private String nodeName;

    private final List<Destination> qDestinations = new ArrayList<>();
    private final List<MessageConsumer> qConsumers = new ArrayList<>();

    protected MessageCallback callback;

    ListenerHolder(Connection brokerConnection, String qName, MessageCallback callback, String nodeName)
    {
        this.nodeName = nodeName;
        this.jmsConnection = brokerConnection;
        this.callback = callback;
        try
        {
            this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
            this.jmsProducer = this.jmsSession.createProducer(null);
            for (String name : qName.split(","))
            {
                this.subscribeTo(name);
            }
        }
        catch (JMSException e)
        {
            throw new ChronixInitializationException("could not create a JMS element", e);
        }
    }

    protected Queue subscribeTo(String qName) throws JMSException
    {
        log.debug(String.format("Registering a message listener on queue %s of %s", qName, callback.getClass()));
        Queue qDestination = this.jmsSession.createQueue(qName);
        MessageConsumer qConsumer = this.jmsSession.createConsumer(qDestination);
        qConsumer.setMessageListener(this);

        this.qConsumers.add(qConsumer);
        this.qDestinations.add(qDestination);

        return qDestination;
    }

    @Override
    public void onMessage(Message arg0)
    {
        if (nodeName != null)
        {
            MDC.put("node", nodeName);
        }

        // This allows the message broker to actually see the classes behind MessageObject messages.
        // It is needed if the broker classes are inside another OSGI bundle.
        Thread.currentThread().setContextClassLoader(Pipeline.class.getClassLoader());

        // The actual actions.
        try
        {
            this.callback.onMessage(arg0, this.jmsSession, this.jmsProducer);
        }
        catch (ListenerRollbackException e)
        {
            log.warn("An error has occured in a JMS message listener. Messages will be roll backed to the queue", e);
            this.jmsRollback();
        }
        catch (Exception e)
        {
            log.warn("An error has occured in a JMS message listener. If any, messages involved will be committed and lost.", e);
        }
        finally
        {
            this.jmsCommit();
        }
    }

    void stopListening()
    {
        log.trace("Stop request received for thread " + this.callback.getClass());
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
        this.qConsumers.clear();
        this.qDestinations.clear();
        this.callback = null;
    }

    protected void jmsCommit()
    {
        try
        {
            jmsSession.commit();
        }
        catch (JMSException e)
        {
            log.error("failure to commit a message consumption in JMS queue linked to consumer " + callback.getClass().getName(), e);
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
            log.error(
                    "Failed to rollback a message consumption or creation in JMS queue linked to consumer " + callback.getClass().getName(),
                    e);
        }
    }
}
