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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.oxymores.chronix.api.agent.ListenerRollbackException;
import org.oxymores.chronix.api.agent.MessageCallback;
import org.oxymores.chronix.core.app.FunctionalSequence;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.context.ChronixContextMeta;
import org.oxymores.chronix.core.context.ChronixContextTransient;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class TranscientListener implements MessageCallback
{
    private static final Logger log = LoggerFactory.getLogger(TranscientListener.class);

    private ChronixContextMeta ctxMeta;
    private ChronixContextTransient ctxDb;
    private String localNodeName;

    TranscientListener(ChronixContextMeta ctxMeta, ChronixContextTransient ctxDb, String localNodeName)
    {
        this.ctxDb = ctxDb;
        this.ctxMeta = ctxMeta;
        this.localNodeName = localNodeName;
    }

    @Override
    public void onMessage(Message msg, Session jmsSession, MessageProducer jmsProducer)
    {
        // Read message (don't commit yet)
        ObjectMessage omsg = (ObjectMessage) msg;
        Object o;
        try
        {
            o = omsg.getObject();

        }
        catch (JMSException e)
        {
            throw new ListenerRollbackException(
                    "An error occurred during transcient reception. Message will stay in queue and will be analysed later", e);
        }

        // ////////////////////////////////////////
        // CalendarPointer
        if (o instanceof CalendarPointer)
        {
            log.debug("A calendar pointer was received");
            CalendarPointer cp = (CalendarPointer) o;
            FunctionalSequence ca;
            State ss = null;

            ca = cp.getCalendar(ctxMeta);
            if (ca == null)
            {
                log.warn("A calendar pointer was received that was unrelated to a locally known calendar - it was ignored");
                return;
            }
            if (cp.getPlaceID() != null)
            {
                ss = cp.getState(ctxMeta);
                if (ss == null)
                {
                    log.warn("A calendar pointer was received that was unrelated to the locally known plan - it was ignored");
                    return;
                }
            }

            // Save it/update it. Beware, id are not the same throughout the network, so query the logical key
            try (Connection conn = this.ctxDb.getTransacDataSource().beginTransaction())
            {
                if (cp.getPlaceID() != null)
                {
                    // Concerning a single state, not the calendar itself.
                    CalendarPointer tmp = conn
                            .createQuery("SELECT * FROM CalendarPointer cp WHERE cp.stateID=:stateID "
                                    + "AND cp.placeID=:placeID AND cp.calendarID=:calendarID")
                            .addParameter("stateID", cp.getStateID()).addParameter("placeID", cp.getPlaceID())
                            .addParameter("calendarID", cp.getCalendarID()).executeAndFetchFirst(CalendarPointer.class);
                    if (tmp != null)
                    {
                        cp.setId(tmp.getId());
                    }
                }
                else
                {
                    // The calendar itself is moving.
                    CalendarPointer tmp = conn
                            .createQuery("SELECT * FROM CalendarPointer cp WHERE cp.stateID IS NULL "
                                    + "AND cp.placeID IS NULL AND cp.calendarID=:calendarID")
                            .addParameter("calendarID", cp.getCalendarID()).executeAndFetchFirst(CalendarPointer.class);
                    if (tmp != null)
                    {
                        cp.setId(tmp.getId());
                    }
                }

                cp.insertOrUpdate(conn);

                // Log
                String represents = "a calendar";
                if (ss != null)
                {
                    represents = ss.getEventSourceDefinition().getName();
                }
                log.debug(String.format(
                        "The calendar pointer is now [Next run %s] [Previous OK run %s] [Previous run %s] [Latest started %s] on [%s]",
                        cp.getNextRunOccurrenceCd(ctxMeta).getLabel(), cp.getLastEndedOkOccurrenceCd(ctxMeta).getLabel(),
                        cp.getLastEndedOccurrenceCd(ctxMeta).getLabel(), cp.getLastStartedOccurrenceCd(ctxMeta).getLabel(), represents));

                // Re analyse events that may benefit from this calendar change
                // All events still there are supposed to be waiting for a new analysis.
                List<State> statesUsingCalendar = ca.getUsedInStates();
                List<String> ids = new ArrayList<>();
                for (State s : statesUsingCalendar)
                {
                    // Events come from states *before* the ones that use the calendar
                    for (State stateFrom : s.getParentStates())
                    {
                        ids.add("'" + stateFrom.getId() + "'");
                    }
                }
                // TODO: remove this horrible join and find a way to use bound parameters...
                List<Event> events = conn.createQuery("SELECT * from Event e WHERE e.stateID IN (" + String.join(",", ids) + ")")
                        .executeAndFetch(Event.class);

                // Send these events for analysis (local only - every execution node has also received this pointer)
                log.info(String.format("The updated calendar pointer may have impacts on %s events that will have to be reanalysed",
                        events.size()));
                for (Event e : events)
                {
                    try
                    {
                        ObjectMessage om = jmsSession.createObjectMessage(e);
                        jmsProducer.send(jmsSession.createQueue(String.format(Constants.Q_EVENT, localNodeName)), om);
                    }
                    catch (JMSException e1)
                    {
                        throw new ListenerRollbackException("Impossible to send an event locally... Will be attempted next reboot", e1);
                    }
                }

                conn.commit();
            }
            log.debug("Saved correctly");

            // Some jobs may now be late (or later than before). Signal them (log only).
            try (Connection conn = this.ctxDb.getTransacDataSource().open())
            {
                ca.processStragglers(conn);
            }
            catch (NullPointerException e1)
            {
                // Pointer updates cannot happen on states without calendars... just ignore it.
                log.debug("Attempt at updating a pointer on a state without calendar", e1);
                return;
            }
        }
        // end calendar pointers
        // /////////////////////////////////////////////////
    }
}
