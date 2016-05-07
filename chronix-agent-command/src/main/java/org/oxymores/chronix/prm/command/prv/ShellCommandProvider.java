package org.oxymores.chronix.prm.command.prv;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.oxymores.chronix.agent.command.api.CommandDescription;
import org.oxymores.chronix.api.agent.MessageListenerService;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.api.prm.ParameterResolutionRequest;
import org.oxymores.chronix.api.source.EventSourceField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ShellCommandProvider implements ParameterProvider
{
    private static final Logger log = LoggerFactory.getLogger(ShellCommandProvider.class);

    ///////////////////////////////////////////////////////////
    // Magic OSGI fields
    private MessageListenerService broker;

    @Reference
    protected void setBroker(MessageListenerService b)
    {
        this.broker = b;
    }
    //
    ///////////////////////////////////////////////////////////

    @Override
    public String getName()
    {
        return "Shell command";
    }

    @Override
    public String getDescription()
    {
        return "the first line of the standard output of a shell command";
    }

    @Override
    public List<EventSourceField> getFields()
    {
        List<EventSourceField> res = new ArrayList<EventSourceField>(3);
        res.add(new EventSourceField("runnerCapability", "the type of runner which should run this command", null, true));
        return res;
    }

    @Override
    public String getValue(ParameterResolutionRequest job)
    {
        log.debug("Received parameter resolution request ID " + job.getRequestId());
        if (job.getFields().get("runnerCapability") == null)
        {
            throw new IllegalArgumentException(
                    "cannot run a command when no capability (field with key 'runnerCapability') has been defined");
        }

        // Just send the command to the command agent
        CommandDescription cd = new CommandDescription();
        cd.setLaunchId(job.getRequestId());
        cd.setRunnerCapability(job.getFields().get("runnerCapability"));
        cd.setStoreLogFile(false);
        for (Map.Entry<String, String> prm : job.getAdditionalParameters())
        {
            cd.addParameter(prm.getKey(), prm.getValue());
        }
        for (Map.Entry<String, String> prm : job.getFields().entrySet())
        {
            cd.addPluginParameter(prm.getKey(), prm.getValue());
        }
        cd.setParameter(true);

        Session s = null;
        try
        {
            s = broker.getNewSession();
            log.debug("Sending a message on queue " + "Q." + job.getNodeName() + ".RUNNER");
            Queue q = s.createQueue("Q." + job.getNodeName() + ".RUNNER");
            Message msg = s.createObjectMessage(cd);
            msg.setJMSReplyTo(s.createQueue(job.getReplyToQueueName()));
            MessageProducer p = s.createProducer(q);
            p.send(msg);
            s.commit();
        }
        catch (Exception e)
        {
            // Well, what could we do
        }
        finally
        {
            if (s != null)
            {
                try
                {
                    s.close();
                }
                catch (JMSException e1)
                {
                    // Nothing to do
                }
            }
        }

        // This plugin is always asynchronous.
        return null;
    }
}