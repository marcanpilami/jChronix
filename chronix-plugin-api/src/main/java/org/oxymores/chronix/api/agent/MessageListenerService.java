package org.oxymores.chronix.api.agent;

import javax.jms.Session;

/**
 * <strong>For advanced plugins which require inter-node communications only.</strong><br>
 * <br>
 * This service provides a mean to share a communication system between Chronix nodes. It is not compulsory to use by any mean - each plugin
 * is allowed to have (should it have need for it) any communication method it likes. But is is recommended to use as this lowers the memory
 * requirements.<br>
 * For the communication system to work, at least one listener must be defined with {@link addListener}. If an active Chronix engine is
 * running, it will have already defined at least one so this will be taken care of in that case. If no engine is running (this is simply an
 * 'agent' node - purely for remote executions) it is the responsibility of the plugin to add an interface. This interface must be
 * modifiable through an OSGI parameter file. The parameter should be optional to avoid adding unnecessary interfaces when running alongside
 * a scheduler engine.<br>
 * <br>
 * An example of use is the shell command plugin: it defines a new data flow for sending the shell commands to run to an execution agent,
 * which can be either alone (a pure remote execution agent node) or running alongside a full engine. This execution agent is waiting for
 * shell commands using this service.<br>
 */
public interface MessageListenerService
{
    /**
     * Register a callback which will be called every time a message arrives on the given message queue.<br>
     * <strong>All registered callbacks must be unregistered on plugin deactivation!</strong>
     * 
     * @return an identifier object that can be used with {@link #removeMessageCallback(Object)}
     * 
     * @param queueName
     *            The name of the message queue to listen to. A convention is to begin the queue name with "Q.". if this queue is specific
     *            to one node (as most queues are), the convention is to begin the name with "Q.NODENAME" instead.
     * @param nodeName
     *            An indication for logs. Can be null.
     */
    public Object addMessageCallback(String queueName, MessageCallback callback, String nodeName);

    /**
     * Disable a callback. The argument is the ID object returned by {@link addMessageCallback}.
     * 
     * @param callbackId
     */
    public void removeMessageCallback(Object callbackId);

    /**
     * Create a two-way channel towards another (remote) node.
     * 
     * @param host
     *            the host (or host:port) to connect to.
     */
    public void addChannel(String host);

    /**
     * For threads in need of an independent JMS session. Since Chronix is a reactive system (it reacts to incoming events), most of its
     * logic is inside message listeners (described as {@link MessageCallback}s). The callbacks already provide a session, so most of the
     * engine does not make use of this method. It is however provided for plugins which may want to be proactive.
     */
    public Session getNewSession();

    /**
     * A helper method mostly for tests. It blocks until the server behind the service has stopped.
     */
    public void waitUntilServerIsStopped();
}
