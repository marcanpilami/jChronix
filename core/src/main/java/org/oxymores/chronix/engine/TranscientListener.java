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

import java.util.ArrayList;
import java.util.List;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.oxymores.chronix.core.Calendar;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.Transition;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.exceptions.ChronixNoCalendarException;

class TranscientListener extends BaseListener
{
    private static Logger log = Logger.getLogger(TranscientListener.class);

    private MessageProducer producerEvent;

    void startListening(Broker broker) throws JMSException
    {
        this.init(broker, true, false);
        log.debug(String.format("(%s) Initializing TranscientListener", ctx.configurationDirectory));

        // Register current object as a listener on LOG queue
        qName = String.format("Q.%s.CALENDARPOINTER", brokerName);
        this.subscribeTo(qName);

        // Producers
        String qEventName = String.format("Q.%s.EVENT", brokerName);
        Destination d = this.jmsSession.createQueue(qEventName);
        this.producerEvent = this.jmsSession.createProducer(d);
    }

    @Override
    public void onMessage(Message msg)
    {
        // Read message (don't commit yet)
        ObjectMessage omsg = (ObjectMessage) msg;
        Object o = null;
        try
        {
            o = omsg.getObject();

        }
        catch (JMSException e)
        {
            log.error("An error occurred during transcient reception. Message will stay in queue and will be analysed later", e);
            jmsRollback();
            return;
        }

        trTransac.begin();

        // ////////////////////////////////////////
        // CalendarPointer
        if (o instanceof CalendarPointer)
        {
            log.debug("A calendar pointer was received");
            CalendarPointer cp = (CalendarPointer) o;
            Calendar ca = null;
            State ss = null;
            try
            {
                ca = cp.getCalendar(ctx);
                if (cp.getPlaceID() != null)
                {
                    ss = cp.getState(ctx);
                }
            }
            catch (Exception e)
            {
                log.error("A calendar pointer was received that was unrelated to the locally known plan - it was ignored");
                trTransac.rollback();
                jmsRollback();
                return;
            }

            // Save it/update it. Beware, id are not the same throughout the network, so query the logical key
            if (cp.getPlaceID() != null)
            {
                TypedQuery<CalendarPointer> q1 = emTransac.createQuery(
                        "SELECT cp FROM CalendarPointer cp WHERE cp.stateID=?1 AND cp.placeID=?2 AND cp.calendarID=?3",
                        CalendarPointer.class);
                q1.setParameter(1, ss.getId().toString());
                q1.setParameter(2, cp.getPlaceID());
                q1.setParameter(3, cp.getCalendarID());
                CalendarPointer tmp = q1.getSingleResult();
                if (tmp != null)
                {
                    cp.setId(tmp.getId());
                }
            }
            else
            {
                TypedQuery<CalendarPointer> q1 = emTransac.createQuery(
                        "SELECT cp FROM CalendarPointer cp WHERE cp.stateID IS NULL AND cp.placeID IS NULL AND cp.calendarID=?1",
                        CalendarPointer.class);
                q1.setParameter(1, cp.getCalendarID());
                CalendarPointer tmp = q1.getSingleResult();
                if (tmp != null)
                {
                    cp.setId(tmp.getId());
                }
            }

            cp = emTransac.merge(cp);

            // Log
            String represents = "a calendar";
            if (cp.getPlaceID() != null)
            {
                represents = ss.getRepresents().getName();
            }
            log.debug(String.format(
                    "The calendar pointer is now [Next run %s] [Previous OK run %s] [Previous run %s] [Latest started %s] on [%s]", cp
                            .getNextRunOccurrenceCd(ctx).getValue(), cp.getLastEndedOkOccurrenceCd(ctx).getValue(), cp
                            .getLastEndedOccurrenceCd(ctx).getValue(), cp.getLastStartedOccurrenceCd(ctx).getValue(), represents));

            // Re analyse events that may benefit from this calendar change
            // All events still there are supposed to be waiting for a new analysis.
            List<State> statesUsingCalendar = ca.getUsedInStates();
            List<String> ids = new ArrayList<String>();
            for (State s : statesUsingCalendar)
            {
                // Events come from states *before* the ones that use the calendar
                for (Transition tr : s.getTrReceivedHere())
                {
                    ids.add(tr.getStateFrom().getId().toString());
                }
            }

            TypedQuery<Event> q = emTransac.createQuery("SELECT e from Event e WHERE e.stateID IN ( :ids )", Event.class);
            q.setParameter("ids", ids);
            List<Event> events = q.getResultList();

            // Send these events for analysis (local only - every execution node
            // has also received this pointer)
            log.info(String.format("The updated calendar pointer may have impacts on %s events that will have to be reanalysed",
                    events.size()));
            for (Event e : events)
            {
                try
                {
                    ObjectMessage om = jmsSession.createObjectMessage(e);
                    producerEvent.send(om);
                }
                catch (JMSException e1)
                {
                    log.error("Impossible to send an event locally... Will be attempted next reboot");
                    jmsRollback();
                    return;
                }
            }

            // Some jobs may now be late (or later than before). Signal them.
            try
            {
                ca.processStragglers(emTransac);
            }
            catch (ChronixNoCalendarException e1)
            {
                // Pointer updates cannot happen on states without calendars... just ignore it.
                jmsCommit();
                return;
            }
        }
        // end calendar pointers
        // /////////////////////////////////////////////////

        // End: commit both JPA and JMS
        trTransac.commit();
        jmsCommit();
        log.debug("Saved correctly");
    }
}
