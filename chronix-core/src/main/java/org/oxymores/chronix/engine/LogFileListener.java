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

import java.io.File;
import java.io.FileOutputStream;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

public class LogFileListener implements MessageListener
{
    private static Logger log = Logger.getLogger(LogFileListener.class);

    private Session jmsSession;
    private Destination logQueueDestination;
    private Connection jmsConnection;
    private MessageConsumer jmsLogConsumer;
    private String logDbPath;

    public void startListening(Connection cnx, String brokerName, String logDbPath) throws JMSException
    {
        log.debug("Initializing Console LogFileListener");

        // Save pointers
        this.jmsConnection = cnx;

        // Register current object as a listener on LOG queue
        String qName = String.format(Constants.Q_LOGFILE, brokerName);
        log.debug(String.format("Broker %s: registering a log listener on queue %s", brokerName, qName));
        this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
        this.logQueueDestination = this.jmsSession.createQueue(qName);
        this.jmsLogConsumer = this.jmsSession.createConsumer(logQueueDestination);
        this.jmsLogConsumer.setMessageListener(this);

        // Log repository
        this.logDbPath = FilenameUtils.normalize(logDbPath);
        if (!(new File(this.logDbPath)).exists())
        {
            (new File(this.logDbPath)).mkdir();
        }
    }

    public void stopListening() throws JMSException
    {
        this.jmsLogConsumer.close();
        this.jmsSession.close();
    }

    @Override
    public void onMessage(Message msg)
    {
        if (msg instanceof BytesMessage)
        {
            BytesMessage bmsg = (BytesMessage) msg;
            String fn = "dump.txt";
            try
            {
                fn = bmsg.getStringProperty("FileName");
            }
            catch (JMSException e)
            {
            }

            try
            {
                int l = (int) bmsg.getBodyLength();
                byte[] r = new byte[l];
                bmsg.readBytes(r);
                IOUtils.write(r, new FileOutputStream(new File(FilenameUtils.concat(this.logDbPath, fn))));
                commit();
            }
            catch (Exception e)
            {
                log.error("An error has occured while receiving a log file. It will be lost. Will not impact the scheduler itself.", e);
                commit();
            }
            log.debug("A console log file was received and saved");
        }
        else
        {
            log.warn("An object was received on the log file queue but was not a log file! Ignored.");
            commit();
            return;
        }

    }

    private void commit()
    {
        try
        {
            jmsSession.commit();
        }
        catch (JMSException e)
        {
            log.error("failure to acknowledge a message in the JMS queue. Oooooops?", e);
            return;
        }
    }
}
