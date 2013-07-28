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

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueBrowser;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

public class SelfTriggerAgentSim extends SelfTriggerAgent
{
	private static Logger log = Logger.getLogger(SelfTriggerAgentSim.class);
	protected DateTime beginTime, endTime;

	@Override
	protected long getNextLoopTime()
	{
		// During a simulation, no need to actually wait for the correct time to come, just loop!
		return 0;
	}

	@Override
	protected void preLoopHook()
	{
		// The clocks can only advance if there are no running elements. That way, multiple runs will not occur simultaneously.
		Long running = this.em.createQuery("SELECT COUNT(e) FROM Event e WHERE e.analysed = False", Long.class).getSingleResult()
				+ this.em.createQuery("SELECT COUNT(p) FROM PipelineJob p WHERE p.status <> 'DONE'", Long.class).getSingleResult();

		QueueBrowser events = null;
		QueueBrowser pjs = null;
		Queue devents, dpjs;
		@SuppressWarnings("rawtypes")
		Enumeration enu = null;
		try
		{
			String event_queue_name = String.format("Q.%s.EVENT", this.ctx.getBrokerName());
			String pj_queue_name = String.format("Q.%s.PJ", this.ctx.getBrokerName());
			devents = this.jmsSession.createQueue(event_queue_name);
			dpjs = this.jmsSession.createQueue(pj_queue_name);
			events = this.jmsSession.createBrowser(devents);
			pjs = this.jmsSession.createBrowser(dpjs);
			enu = events.getEnumeration();
			while (enu.hasMoreElements())
			{
				enu.nextElement();
				running++;
			}
			enu = pjs.getEnumeration();
			while (enu.hasMoreElements())
			{
				enu.nextElement();
				running++;
			}
			pjs.close();
			events.close();
		} catch (JMSException e1)
		{
			e1.printStackTrace();
			return;
		}

		while (running > 0)
		{
			try
			{
				Thread.sleep(500);
			} catch (InterruptedException e)
			{
			}

			running = this.em.createQuery("SELECT COUNT(e) FROM Event e WHERE e.analysed = False", Long.class).getSingleResult()
					+ this.em.createQuery("SELECT COUNT(p) FROM PipelineJob p WHERE p.status <> 'DONE'", Long.class).getSingleResult();
			log.debug(String.format("There are %s elements unack in the db", running));

			try
			{
				events = this.jmsSession.createBrowser(devents);
				pjs = this.jmsSession.createBrowser(dpjs);
				enu = events.getEnumeration();
				while (enu.hasMoreElements())
				{
					enu.nextElement();
					running++;
				}
				enu = pjs.getEnumeration();
				while (enu.hasMoreElements())
				{
					enu.nextElement();
					running++;
				}
				pjs.close();
				events.close();
			} catch (JMSException e1)
			{
				e1.printStackTrace();
			}
			log.debug(String.format("There are %s elements unack in the db + mq", running));

		}

		// Here, should trigger external events
		// TODO: external events simulation

		// Exit if we are now out of the simulation window
		if (this.endTime.isBefore(this.nextLoopVirtualTime))
		{
			log.debug("STAS will stop");
			try
			{
				this.producerEvents.close();
				this.jmsSession.close();
			} catch (JMSException e)
			{
				e.printStackTrace();
			}
			run = false;
			loop.release();
		}
	}

	public DateTime getBeginTime()
	{
		return beginTime;
	}

	public void setBeginTime(DateTime beginTime)
	{
		this.beginTime = beginTime;
		this.nextLoopVirtualTime = beginTime;
	}

	public DateTime getEndTime()
	{
		return endTime;
	}

	public void setEndTime(DateTime endTime)
	{
		log.debug("End of simulation : " + endTime);
		this.endTime = endTime;
	}
}
