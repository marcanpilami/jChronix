package org.oxymores.chronix.api.agent;

import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * The interface to implement so as to be able to register inside the {@link MessageListenerService}.
 */
public interface MessageCallback
{
    /**
     * This method is the callback from which the interface is named. It is called from within an active {@link Session} with an opened
     * transaction. This transaction is always committed by the {@link MessageListenerService} at the end of this method, even if this
     * method ends in an Exception, unless it is an {@link ListenerRollbackException} (in which case the transaction is rollbacked). So
     * there is usually no need to explicitly call commit or rollback on the session.<br>
     * <br>
     * Do not ever close the session, as it would stop your listener!<br>
     * <br>
     * The given {@link Session} is mostly provided to allow implementing classes to send their own messages from within the same
     * transaction. The {@link MessageProducer} is also given for this reason (it is actually an optimisation allowing the reuse of the same
     * producer. Implementing classes are free to ignore it and create their owns from the session).
     * 
     * @param msg
     * @param jmsSession
     * @param producer
     */
    public void onMessage(Message msg, Session jmsSession, MessageProducer producer);

}
