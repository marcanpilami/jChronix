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

package org.oxymores.chronix.engine.analyser;

import java.util.HashSet;
import java.util.Set;

import org.oxymores.chronix.core.CalendarDay;
import org.oxymores.chronix.core.EventSourceWrapper;
import org.oxymores.chronix.core.Place;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.context.Application;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.transactional.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

class TransitionOnSinglePlaceAnalyser
{
    private static final Logger log = LoggerFactory.getLogger(TransitionOnSinglePlaceAnalyser.class);

    /**
     * The actual result
     */
    boolean allowed = false;

    /**
     * The events that have been consumed (only if transition is possible, empty otherwise)
     */
    Set<Event> consumedEvents = new HashSet<Event>();

    TransitionOnSinglePlaceAnalyser(Application application, DTOTransition tr, EventSourceWrapper src, Set<Event> events, Place p,
            Connection conn)
    {
        for (Event e : events)
        {
            if (!e.getPlaceID().equals(p.id))
            {
                // Only accept events from the analysed place
                continue;
            }

            if (!e.getStateID().equals(tr.getFrom()))
            {
                // Only accept events from the analysed state
                continue;
            }

            // Check guards (according to source plugin)
            if (!src.isTransitionPossible(tr, e))
            {
                continue;
            }

            // Check calendar if the transition is calendar-aware
            if (tr.isCalendarAware())
            {
                State stateTo = application.getState(tr.getTo());
                log.debug("Checking wether an event respects a calendar transition guard");
                if (!stateTo.usesCalendar())
                {
                    // No calendar used on the target - yet the transition must make sure a calendar is enforced...
                    continue;
                }

                if (e.getIgnoreCalendarUpdating())
                {
                    // Calendar is forced - this is deliberate from the user, he's supposed to know what he does so no checks
                }
                else
                {
                    if (e.getCalendarOccurrenceID() == null)
                    {
                        continue;
                    }

                    try
                    {
                        if (!e.getCalendarOccurrenceID().equals(stateTo.getCurrentCalendarPointer(conn, p).getNextRunOccurrenceId()))
                        {
                            CalendarDay cd1 = stateTo.getCalendar().getDay(e.getCalendarOccurrenceID());
                            CalendarDay cd2 = stateTo.getCalendar()
                                    .getDay(stateTo.getCurrentCalendarPointer(conn, p).getNextRunOccurrenceId());
                            log.debug(String.format("Rejected an event for date mismatch: got %s (in event) expected %s (in target state)",
                                    cd1.getValue(), cd2.getValue()));
                            continue;
                        }
                    }
                    catch (Exception e1)
                    {
                        log.error(
                                "An event was rejected on a transition because the calendar analyis encountered an issue. It may not be an issue, yet there is likely a problem with the plan that should be corrected.",
                                e1);
                        continue;
                    }
                }
            }

            // If here: the event is OK for the given transition on the given
            // place.
            this.consumedEvents.add(e);
            this.allowed = true;
            return; // no need to continue as analysis already has OK result
        }
        // If here, analysis ends on KO.
    }
}
