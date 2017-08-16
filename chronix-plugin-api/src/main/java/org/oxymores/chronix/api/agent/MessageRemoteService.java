package org.oxymores.chronix.api.agent;

import javax.jms.Message;

/**
 * A very simple service allowing to send a message to a remote queue manager. This should only be called for bootstrap operations, as all
 * chronix nodes should always have a broker nearby and use its method to send messages.
 *
 */
public interface MessageRemoteService
{
    /**
     * Simple synchronous service which will connect to the specified broker, send a text message, await a response and give back the
     * received response message.<br>
     * <br>
     * Be wary of using this - it is a synchronous call for an asynchronous system! Often an antipattern.<br>
     * 
     * @param textToSend
     *            content of the message
     * @param remoteHost
     *            DNS or IP of the broker
     * @param remotePort
     *            port of the broker
     * @param destQueueName
     *            name of the destination queue
     * @return the response message
     */
    public Message sendText(String textToSend, String remoteHost, int remotePort, String destQueueName);
}
