package org.oxymores.chronix.core.context;

import java.io.Serializable;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;

import org.oxymores.chronix.api.source.DTOEventSource;
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.EngineCallback;
import org.oxymores.chronix.api.source.EventSourceRunResult;
import org.oxymores.chronix.core.app.Application;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.data.RunResult;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.engine.helpers.SenderHelpers.JmsSendData;
import org.oxymores.chronix.exceptions.ChronixException;
import org.sql2o.Connection;

public class EngineCbRun implements EngineCallback
{
    private ChronixEngine e;
    private ChronixContextMeta ctxMeta;
    private PipelineJob pj;
    private Application a;

    public EngineCbRun(ChronixEngine e, ChronixContextMeta ctx, Application a, PipelineJob pj)
    {
        this.e = e;
        this.ctxMeta = ctx;
        this.a = a;
        this.pj = pj;
    }

    @Override
    public void sendMessage(Serializable msg, String destinationQueue, String replyQueue)
    {
        try (JmsSendData d = new JmsSendData())
        {
            ObjectMessage omsg = d.jmsSession.createObjectMessage(msg);
            omsg.setJMSReplyTo(d.jmsSession.createQueue(replyQueue));
            d.jmsProducer.send(d.jmsSession.createQueue(destinationQueue), omsg);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void sendRunResult(EventSourceRunResult r)
    {
        RunResult rr = new RunResult(pj, r);

        try
        {
            SenderHelpers.sendRunResult(rr, this.e.getLocalNode().getName());
        }
        catch (JMSException e)
        {
            throw new ChronixException("could not send message", e);
        }
    }

    @Override
    public DTOEventSource getEventSource(UUID id)
    {
        return this.a.getEventSource(id).getDTO();
    }

    @Override
    public void launchState(DTOState s)
    {
        try (Connection o = this.e.getContextTransient().getTransacDataSource().beginTransaction(); JmsSendData d = new JmsSendData())
        {
            a.getState(s.getId()).runInsidePlan(o, d.jmsProducer, d.jmsSession, pj.getId(), null, pj.getVirtualTime());
            d.jmsSession.commit();
        }
        catch (Exception e1)
        {
            throw new ChronixException("could not launch a state", e1);
        }
    }

    @Override
    public String getResultQueueName()
    {
        return String.format(Constants.Q_RUNNERMGR, e.getLocalNode().getName());
    }

    @Override
    public String getNodeName()
    {
        return e.getLocalNode().getName();
    }
}
