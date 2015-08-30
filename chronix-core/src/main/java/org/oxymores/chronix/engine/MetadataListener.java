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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.slf4j.LoggerFactory;

class MetadataListener extends BaseListener
{
    private static final Logger log = LoggerFactory.getLogger(MetadataListener.class);
    private ChronixEngine engine;

    void startListening(Broker b, ChronixEngine engine) throws JMSException
    {
        this.init(b);
        log.debug(String.format("Initializing MetadataListener"));

        // Pointers
        this.engine = engine;

        // Listen for applications
        this.qName = String.format(Constants.Q_META, brokerName);
        this.subscribeTo(qName);
    }

    @Override
    public void onMessage(Message msg)
    {
        log.debug("A metadata object was received");
        ObjectMessage omsg = (ObjectMessage) msg;
        Application a = null;
        Environment n = null;
        boolean restart = true;
        try
        {
            restart = !msg.getBooleanProperty("dont_restart");
        }
        catch (JMSException e)
        {
            // Nothing to do - default value is ued if property is absent.
        }

        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Application) && !(o instanceof Environment))
            {
                log.warn("An object was received on the metadata queue but was not an app or an environment specification! Ignored.");
                jmsCommit();
                return;
            }

            if (o instanceof Application)
            {
                a = (Application) o;
            }
            else
            {
                n = (Environment) o;
            }
        }
        catch (JMSException e)
        {
            log.error("An error occurred during metadata message reception. This message will be retried later.", e);
            jmsRollback();
            return;
        }

        // Application
        if (a != null)
        {
            try
            {
                log.debug("Saving received app as the current working copy");
                ctx.saveApplication(a);
            }
            catch (Exception e1)
            {
                log.error(
                        "Issue while trying to commit to disk an application received from another node. The application sent will be thrown out.",
                        e1);
                jmsCommit();
            }
        }

        // Environment
        if (n != null)
        {
            try
            {
                ctx.saveEnvironment(n);
            }
            catch (ChronixPlanStorageException ex)
            {
                log.error("Could not store received environment specification. It will be ignored.", ex);
            }
        }

        // Data storage done - ack the queue.
        jmsCommit();

        // Recycle engine.
        if (restart)
        {
            engine.queueReloadConfiguration();
        }
        log.debug("Metadata was correctly received, engine will now reload its configuration");
    }
}
