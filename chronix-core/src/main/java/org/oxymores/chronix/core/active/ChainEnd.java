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
package org.oxymores.chronix.core.active;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.slf4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.ActiveNodeBase;
import org.oxymores.chronix.core.ChronixContext;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class ChainEnd extends ActiveNodeBase
{
    private static final Logger log = LoggerFactory.getLogger(ChainEnd.class);
    private static final long serialVersionUID = 4129809921422152571L;

    public ChainEnd()
    {
        this.name = "Chain end";
    }

    @Override
    public boolean visibleInHistory()
    {
        return false;
    }

    @Override
    public void internalRun(Connection conn, ChronixContext ctx, PipelineJob pj, MessageProducer jmsProducer, Session jmsSession, DateTime virtualTime)
    {
        RunResult rr = new RunResult();
        rr.returnCode = 0;
        rr.logStart = this.name;
        rr.id1 = pj.getLevel2Id();
        rr.end = virtualTime;

        try
        {
            ObjectMessage msg = jmsSession.createObjectMessage(rr);
            String qName = String.format(Constants.Q_ENDOFJOB, pj.getPlace(ctx).getNode().getComputingNode().getBrokerName());
            log.info(String.format("An end of job signal for the chain run %s will be sent to the runner over the wire on queue %s",
                    rr.id1, qName));
            Destination d = jmsSession.createQueue(qName);
            jmsProducer.send(d, msg);
        }
        catch (Exception e)
        {
            log.error("An error occurred during end of chain processing - the plan will be botched", e);
        }
    }
}
