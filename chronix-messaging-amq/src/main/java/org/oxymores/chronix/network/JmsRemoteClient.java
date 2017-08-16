package org.oxymores.chronix.network;

import java.util.UUID;
import java.util.concurrent.Semaphore;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.ws.Holder;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.osgi.service.component.annotations.Component;
import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageRemoteService;
import org.oxymores.chronix.exceptions.ChronixMessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(configurationPid = "ServiceRemoteClient", immediate = false)
public class JmsRemoteClient implements MessageRemoteService
{
    private final static Logger log = LoggerFactory.getLogger(JmsRemoteClient.class);

    synchronized public Message sendText(String textToSend, String remoteHost, int remotePort, String destQueueName)
    {
        ActiveMQConnectionFactory factory;
        Connection conn = null;
        final Holder<String> corelId = new Holder<>();
        final Semaphore ended = new Semaphore(0);

        factory = new ActiveMQConnectionFactory("tcp://" + remoteHost + ":" + remotePort);
        final Holder<Message> res = new Holder<>();
        MessageConsumer responseConsumer;
        Session jmsSession;

        try
        {
            // Try to connect 5 times
            int nbErrors = 0;
            while (conn == null)
            {
                try
                {
                    conn = factory.createConnection();
                }
                catch (JMSException e)
                {
                    nbErrors++;
                    if (nbErrors > 5)
                    {
                        throw e;
                    }
                    log.info("Could not connect to remote host. Will retry (" + (5 - nbErrors) + " retries left).");
                    Thread.sleep(1000);
                }
            }

            conn.start();
            jmsSession = conn.createSession(true, Session.SESSION_TRANSACTED);

            // Answer queue and listener
            Destination tempDest = jmsSession.createTemporaryQueue();
            responseConsumer = jmsSession.createConsumer(tempDest);

            responseConsumer.setMessageListener(new MessageListener()
            {
                @Override
                public void onMessage(Message message)
                {
                    try
                    {
                        if (!message.getJMSCorrelationID().equals(corelId.value))
                        {
                            log.warn(
                                    "Received a message that was not an answer to the current request - may come from a previous engine launch. Is ignored.");
                            jmsSession.commit();
                            return;
                        }
                    }
                    catch (Exception ex1)
                    {
                        // Ignore
                    }

                    try
                    {
                        res.value = message;
                        jmsSession.commit();
                    }
                    catch (ListenerRollbackException e)
                    {
                        try
                        {
                            jmsSession.rollback();

                        }
                        catch (JMSException ex2)
                        {
                            log.error("failure to rollback reading answer to a request for metadata in JMS queue", ex2);
                        }
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            jmsSession.commit();
                        }
                        catch (Exception ex)
                        {
                            // Ignore silently.
                        }
                    }
                    finally
                    {
                        ended.release();
                    }
                }
            });

            Destination qDestination = jmsSession.createQueue(destQueueName);
            MessageProducer producer = jmsSession.createProducer(qDestination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            TextMessage txtMessage = jmsSession.createTextMessage();
            txtMessage.setText(textToSend);
            txtMessage.setJMSReplyTo(tempDest);
            txtMessage.setStringProperty("BS", "present");
            corelId.value = UUID.randomUUID().toString();
            txtMessage.setJMSCorrelationID(corelId.value);
            producer.send(txtMessage);

            try
            {
                jmsSession.commit();
            }
            catch (JMSException e)
            {
                log.error("failure to commit a request for metadata in JMS queue", e);
            }
            producer.close();
        }
        catch (JMSException | InterruptedException ex)
        {
            log.error("Failed to do basic JMS request/answer", ex);
            throw new ChronixMessagingException("Failed to do basic JMS request/answer", ex);
        }

        // Wait for the message listener to put a unit in the semaphore... it means message was received.
        try
        {
            ended.acquire();
        }
        catch (InterruptedException ex)
        {
            log.warn("Wait for environment answer was interrupted", ex);
        }

        // Clean. No reuse in this service.
        try
        {
            responseConsumer.close();
            jmsSession.close();
            conn.close();
        }
        catch (JMSException ex)
        {
            log.warn("Could not close JMS connections after fetching the environment file from console. Implies a possible resource leak.",
                    ex);
        }
        return res.value;
    }
}
