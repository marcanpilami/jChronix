package org.oxymores.chronix.core.context;

import java.util.UUID;

import javax.jms.JMSException;

import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.oxymores.chronix.engine.helpers.SenderHelpers.JmsSendData;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.oxymores.chronix.exceptions.ChronixException;
import org.sql2o.Connection;

public class EngineCbRun implements EngineCallback
{
    private ChronixEngine e;
    private ChronixContextMeta ctxMeta;
    private PipelineJob pj;
    private Application2 a;

    public EngineCbRun(ChronixEngine e, ChronixContextMeta ctx, Application2 a, PipelineJob pj)
    {
        this.e = e;
        this.ctxMeta = ctx;
        this.a = a;
        this.pj = pj;
    }

    @Override
    public void sendMessageXCXXXXX(Object msg, String destinationQueue)
    {
        // TODO Auto-generated method stub
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
    public EventSource getEventSource(UUID id)
    {
        return this.a.getEventSource(id);
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
}
