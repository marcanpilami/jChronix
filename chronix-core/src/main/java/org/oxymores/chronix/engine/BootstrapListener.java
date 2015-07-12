/**
 * By Marc-Antoine Gouillart, 2015
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

import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.Network;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;

/**
 This class is only called when an engine starts without a network file file and is part of a network.
 Its role is to fetch the network file from a known other engine.<br>
 As it is called before the broker is created, it is the only listener that directly connects to a remote broker.
 */
public class BootstrapListener implements MessageListener
{
    private final static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(BootstrapListener.class);

    private final ChronixContext ctx;
    private final String localNodeName;
    private boolean ok = false;
    private final Semaphore ended = new Semaphore(0);

    private final String remoteHost;
    private final int remotePort;
    ActiveMQConnectionFactory factory;
    Connection conn;
    Session jmsSession;
    Queue qDestination;
    private MessageConsumer responseConsumer;
    private String corelId;

    public BootstrapListener(ChronixContext ctx, String localNodeName, String remoteHost, int remotePort)
    {
        this.ctx = ctx;
        this.localNodeName = localNodeName;
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    public boolean fetchNetwork()
    {
        log.info("Will try to get network file from a known remote host at " + this.remoteHost + ":" + this.remotePort);
        this.factory = new ActiveMQConnectionFactory("tcp://" + this.remoteHost + ":" + this.remotePort);
        try
        {
            this.conn = this.factory.createConnection();
            this.conn.start();
            this.jmsSession = this.conn.createSession(true, Session.SESSION_TRANSACTED);

            Destination tempDest = this.jmsSession.createTemporaryQueue();
            this.responseConsumer = this.jmsSession.createConsumer(tempDest);
            this.responseConsumer.setMessageListener(this);

            this.qDestination = this.jmsSession.createQueue(Constants.Q_BOOTSTRAP);
            MessageProducer producer = this.jmsSession.createProducer(this.qDestination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            TextMessage txtMessage = this.jmsSession.createTextMessage();
            txtMessage.setText(this.localNodeName);
            txtMessage.setJMSReplyTo(tempDest);
            txtMessage.setStringProperty("BS", "present");
            this.corelId = UUID.randomUUID().toString();
            txtMessage.setJMSCorrelationID(this.corelId);
            producer.send(txtMessage);
            jmsCommit();
            producer.close();
        }
        catch (JMSException ex)
        {
            log.fatal("Could not fetch netwok definition from console", ex);
            return false;
        }

        join();
        return ok;
    }

    @Override
    public void onMessage(Message message)
    {
        if (!(message instanceof ObjectMessage))
        {
            log.error("Received a message that was not an ObjectMessage on bootstrap temporary queue. Will continue to wait for a good answer.");
            jmsCommit();
            return;
        }

        ObjectMessage omsg = (ObjectMessage) message;
        try
        {
            if (!omsg.getJMSCorrelationID().equals(corelId))
            {
                log.warn("Received a message that was not an answer to the current request - may come from a previous engine launch. Is ignored.");
                jmsCommit();
                return;
            }

            Object o = omsg.getObject();
            if (!(o instanceof Network))
            {
                log.fatal("Received an answer but of the wrong type - engine cannot start");
                jmsCommit();
                stop();
            }

            Network n = (Network) o;
            log.info("network was received from remote node and will now be stored to disk");
            ctx.saveNetwork(n);
            ok = true;
            stop();
        }
        catch (JMSException | ChronixPlanStorageException ex)
        {
            Logger.getLogger(BootstrapListener.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void stop()
    {
        try
        {
            responseConsumer.close();
            jmsSession.close();
            conn.close();
        }
        catch (JMSException ex)
        {
            log.warn("Could not close JMS connections after fetching the network file from console. Implies a possible resource leak.", ex);
        }
        ended.release();
    }

    private void join()
    {
        try
        {
            ended.acquire();
        }
        catch (InterruptedException ex)
        {
            log.warn("Wait for network answer was interrupted", ex);
        }
    }

    protected void jmsCommit()
    {
        try
        {
            jmsSession.commit();
        }
        catch (JMSException e)
        {
            log.error("failure to commit an event consumption in JMS queue"
                    + ". Scheduler will now abort as it is a dangerous situation. You may need to empty all queues before restarting.", e);
        }
    }
}
