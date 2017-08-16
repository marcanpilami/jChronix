package org.oxymores.chronix.engine.helpers;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;

import org.oxymores.chronix.api.agent.MessageRemoteService;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.exceptions.ChronixInitializationException;
import org.oxymores.chronix.exceptions.ChronixPlanStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is only called when an engine starts without an environment file file and is part of a network. Its role is to fetch the
 * environment file from a known other engine.<br>
 * As it is called before the broker is created, it is the only part of then engine which directly connects to a remote broker.
 */
public final class Bootstrap
{
    private final static Logger log = LoggerFactory.getLogger(Bootstrap.class);

    public static Environment fetchData(MessageRemoteService svc, String localNodeName, String remoteHost, int remotePort)
    {
        Message message = svc.sendText(localNodeName, remoteHost, remotePort, Constants.Q_BOOTSTRAP);

        if (!(message instanceof ObjectMessage))
        {
            throw new ChronixInitializationException("Received a message that was not an ObjectMessage on bootstrap temporary queue");
        }

        ObjectMessage omsg = (ObjectMessage) message;
        try
        {
            Object o = omsg.getObject();
            if (!(o instanceof Environment))
            {
                throw new ChronixInitializationException("Received an answer but of the wrong type - engine cannot start");
            }

            Environment n = (Environment) o;
            log.info("environment was received correctly from remote node");
            return n;
        }
        catch (JMSException | ChronixPlanStorageException ex)
        {
            throw new ChronixInitializationException("Generic error when receiving bootstrap message", ex);
        }
    }
}
