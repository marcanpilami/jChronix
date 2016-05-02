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
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MetadataListener implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(MetadataListener.class);
    private ChronixEngine engine;
    private ChronixContextMeta ctxMeta;

    MetadataListener(ChronixEngine e, ChronixContextMeta ctxMeta)
    {
        this.ctxMeta = ctxMeta;
        this.engine = e;
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
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
            throw new ListenerRollbackException("An error occurred during metadata message reception. This message will be retried later.",
                    e);
        }

        // Application
        if (a != null)
        {
            try
            {
                log.debug("Saving received app as the current working copy");
                ctxMeta.saveApplicationDraft(a);
                ctxMeta.activateApplicationDraft(a.getId(), "x");
            }
            catch (Exception e1)
            {
                log.error(
                        "Issue while trying to commit to disk an application received from another node. The application sent will be thrown out.",
                        e1);
            }
        }

        // Environment
        if (n != null)
        {
            try
            {
                ctxMeta.saveEnvironmentDraft(n);
                ctxMeta.activateEnvironmentDraft("F");
            }
            catch (ChronixPlanStorageException ex)
            {
                log.error("Could not store received environment specification. It will be ignored.", ex);
            }
        }

        // Recycle engine.
        if (restart)
        {
            engine.queueReloadConfiguration();
        }
        log.debug("Metadata was correctly received, engine will now reload its configuration");
    }
}
