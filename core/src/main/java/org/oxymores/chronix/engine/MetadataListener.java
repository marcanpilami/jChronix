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

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ChronixContext;

class MetadataListener implements MessageListener
{
	private static Logger log = Logger.getLogger(MetadataListener.class);

	private ChronixContext ctx;
	private ChronixEngine engine;
	private Session jmsSession;
	private Destination jmsApplicationQueue;
	private Connection jmsConnection;
	private MessageConsumer jmsApplicationConsumer;

	void startListening(Connection cnx, String brokerName, ChronixContext ctx, ChronixEngine engine) throws JMSException
	{
		log.debug(String.format("(%s) Initializing MetadataListener", ctx.configurationDirectory));

		// Pointers
		this.ctx = ctx;
		this.jmsConnection = cnx;
		this.engine = engine;

		// Listen for applications
		String qName = String.format("Q.%s.APPLICATION", brokerName);
		log.debug(String.format("Broker %s: registering a metadata listener on queue %s", brokerName, qName));
		this.jmsSession = this.jmsConnection.createSession(true, Session.SESSION_TRANSACTED);
		this.jmsApplicationQueue = this.jmsSession.createQueue(qName);
		this.jmsApplicationConsumer = this.jmsSession.createConsumer(this.jmsApplicationQueue);
		this.jmsApplicationConsumer.setMessageListener(this);
	}

	void stopListening() throws JMSException
	{
		this.jmsApplicationConsumer.close();
		this.jmsSession.close();
	}

	@SuppressWarnings("finally")
	@Override
	public void onMessage(Message msg)
	{
		log.debug(String.format("An application was received (local node db is %s)", ctx.configurationDirectory));
		ObjectMessage omsg = (ObjectMessage) msg;
		Application a;
		try
		{
			Object o = omsg.getObject();
			if (!(o instanceof Application))
			{
				log.warn("An object was received on the app queue but was not an app! Ignored.");
				return;
			}

			a = (Application) o;
		} catch (JMSException e)
		{
			log.error("An error occurred during metadata message reception. BAD. Will go on", e);
			return;
		}

		try
		{
			log.debug("Saving received app as the current working copy");
			ctx.saveApplication(a);
		} catch (Exception e1)
		{
			log.error(
					"Issue while trying to commit to disk an application received from another node. The application sent will be thrown out.",
					e1);
			try
			{
				jmsSession.commit(); // if read again, would still fail.
			} finally
			{
				return;
			}
		}

		try
		{
			log.debug("Setting the new app version as the active version");
			ctx.setWorkingAsCurrent(a);
		} catch (Exception e1)
		{
			log.error(
					"An application was correctly received and saved to disk. However, it could not be activated, which requires a file to be copied. Check log and try again sending the application (or activate it manually.)",
					e1);
		}

		try
		{
			jmsSession.commit();
		} catch (JMSException e)
		{
			log.error(
					"While receiving an application definition, we could not commit reading the message. It is a catastrophy: the db has already been correctly commited with the application data. The scheduler will stop. Empty the APPLICATION queue and restart it.",
					e);
			return;
		}

		// Recycle engine.
		engine.queueReloadConfiguration();

		log.debug(String.format("Application of id %s received", a.getId()));
	}
}
