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

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Application;

class MetadataListener extends BaseListener
{
    private static Logger log = Logger.getLogger(MetadataListener.class);
    private ChronixEngine engine;

    void startListening(Broker b, ChronixEngine engine) throws JMSException
    {
        this.init(b, false, false);
        log.debug(String.format("(%s) Initializing MetadataListener", ctx.getContextRoot()));

        // Pointers
        this.engine = engine;

        // Listen for applications
        this.qName = String.format(Constants.Q_META, brokerName);
        this.subscribeTo(qName);
    }

    @Override
    public void onMessage(Message msg)
    {
        log.debug(String.format("An application was received (local node db is %s)", ctx.getContextRoot()));
        ObjectMessage omsg = (ObjectMessage) msg;
        Application a;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Application))
            {
                log.warn("An object was received on the app queue but was not an app! Ignored.");
                jmsCommit();
                return;
            }

            a = (Application) o;
        }
        catch (JMSException e)
        {
            log.error("An error occurred during metadata message reception. This message will be retried later.", e);
            jmsRollback();
            return;
        }

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

        try
        {
            log.debug("Setting the new app version as the active version");
            ctx.setWorkingAsCurrent(a);
        }
        catch (Exception e1)
        {
            log.error(
                    "An application was correctly received and saved to disk. However, it could not be activated, which requires a file to be copied. Check log and try again sending the application (or activate it manually.)",
                    e1);
        }

        jmsCommit();

        // Recycle engine.
        engine.queueReloadConfiguration();
        log.debug(String.format("Application of id %s received", a.getId()));
    }
}
