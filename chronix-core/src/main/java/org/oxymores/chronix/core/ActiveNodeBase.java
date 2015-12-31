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
package org.oxymores.chronix.core;

import java.util.ArrayList;
import java.util.List;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.NotImplementedException;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.RunnerManager;
import org.oxymores.chronix.engine.data.EventAnalysisResult;
import org.oxymores.chronix.engine.data.PlaceAnalysisResult;
import org.oxymores.chronix.engine.data.TransitionAnalysisResult;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.oxymores.chronix.exceptions.ChronixRunException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class ActiveNodeBase extends NamedApplicationObject
{
    private static final long serialVersionUID = 2317281646089939267L;
    private static final Logger log = LoggerFactory.getLogger(ActiveNodeBase.class);

    @NotNull
    protected ArrayList<Parameter> parameters;

    public ActiveNodeBase()
    {
        super();
        parameters = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", name, description);
    }

    // Helper function (could be overloaded) returning something intelligible
    // designating the element that is run by this source
    public String getCommandName(PipelineJob pj, RunnerManager sender, ChronixContext ctx)
    {
        return null;
    }

    // stupid get/set
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // Relationship traversing
    public List<State> getClientStates()
    {
        ArrayList<State> res = new ArrayList<>();
        for (State s : this.application.getStates())
        {
            if (s.represents == this)
            {
                res.add(s);
            }
        }
        return res;
    }

    // Relationship traversing
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // Parameter handling
    public ArrayList<Parameter> getParameters()
    {
        return this.parameters;
    }

    public void addParameter(Parameter parameter)
    {
        if (!parameters.contains(parameter))
        {
            parameters.add(parameter);
        }
    }

    public void addParameter(String key, String value, String description)
    {
        Parameter p = new Parameter();
        p.setDescription(description);
        p.setKey(key);
        p.setValue(value);
        this.application.addParameter(p);
        addParameter(p);
    }
    // Parameter handling
    // ////////////////////////////////////////////////////////////////////////////

    // ////////////////////////////////////////////////////////////////////////////
    // Event analysis
    // Do the given events allow for a transition originating from a state
    // representing this source?
    public PlaceAnalysisResult createdEventRespectsTransitionOnPlace(Transition tr, List<Event> events, Place p, Connection conn)
    {
        PlaceAnalysisResult res = new PlaceAnalysisResult(p);
        res.res = false;

        for (Event e : events)
        {
            if (!e.getPlaceID().equals(p.id))
            {
                // Only accept events from the analyzed place
                continue;
            }

            if (!e.getStateID().equals(tr.stateFrom.id))
            {
                // Only accept events from the analyzed state
                continue;
            }

            // Check guards
            if (tr.guard1 != null && !tr.guard1.equals(e.getConditionData1()))
            {
                continue;
            }
            if (tr.guard2 != null && !tr.guard2.equals(e.getConditionData2()))
            {
                continue;
            }
            if (tr.guard3 != null && !tr.guard3.equals(e.getConditionData3()))
            {
                continue;
            }
            if (tr.guard4 != null && !tr.guard4.equals(e.getConditionData4()))
            {
                continue;
            }

            // Check calendar if the transition is calendar-aware
            if (tr.calendarAware)
            {
                log.debug("Checking wether an event respects a calendar transition guard");
                if (!tr.stateTo.usesCalendar())
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
                        if (!e.getCalendarOccurrenceID().equals(tr.stateTo.getCurrentCalendarPointer(conn, p).getNextRunOccurrenceId()))
                        {
                            CalendarDay cd1 = tr.stateTo.getCalendar().getDay(e.getCalendarOccurrenceID());
                            CalendarDay cd2 = tr.stateTo.getCalendar().getDay(tr.stateTo.getCurrentCalendarPointer(conn, p).getNextRunOccurrenceId());
                            log.debug(String.format("Rejected an event for date mismatch: got %s (in event) expected %s (in target state)",
                                    cd1.seq, cd2.seq));
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
            res.consumedEvents.add(e);
            res.usedEvents.add(e);
            res.res = true;
            return res;
        }
        return res;
    }

    public EventAnalysisResult isStateExecutionAllowed(State s, Event evt, Connection conn, MessageProducer pjProducer, Session session,
            ChronixContext ctx)
    {
        EventAnalysisResult res = new EventAnalysisResult(s);

        // Get session events
        List<Event> sessionEvents2 = conn.createQuery("SELECT e.* FROM Event e WHERE e.level0Id = :level0Id AND e.level1Id = :level1Id")
                .addParameter("level0Id", evt.getLevel0Id()) // $The unique ID associated to a chain run instance$
                .addParameter("level1Id", evt.getLevel1Id()) // $The chain ID$
                .executeAndFetch(Event.class);

        // Remove consumed events (first filter: those which are completely consumed)
        List<Event> sessionEvents = new ArrayList<>();
        for (Event e : sessionEvents2)
        {
            for (Place p : s.runsOn.places)
            {
                if (!sessionEvents.contains(e) && !e.wasConsumedOnPlace(p, s, conn))
                {
                    sessionEvents.add(e);
                }
            }
        }

        // The current event may not yet be DB persisted
        if (!sessionEvents.contains(evt))
        {
            sessionEvents.add(evt);
        }
        log.debug("There are " + sessionEvents.size() + " events not consumed to be considered for analysis");

        // Check every incoming transition: are they allowed?
        for (Transition tr : s.getTrReceivedHere())
        {
            log.debug(String.format("State %s (%s - chain %s) analysis with %s events", s.getId(), s.represents.getName(),
                    s.chain.getName(), sessionEvents.size()));

            TransitionAnalysisResult tar = tr.isTransitionAllowed(sessionEvents, conn);

            if (tar.totallyBlocking() && this.multipleTransitionHandling() == MultipleTransitionsHandlingMode.AND)
            {
                // No need to go further - one transition will block everything
                log.debug(String.format("State %s (%s - chain %s) is NOT allowed to run due to transition from %s", s.getId(),
                        s.represents.getName(), s.chain.getName(), tr.stateFrom.represents.name));
                return new EventAnalysisResult(s);
            }

            res.analysis.put(tr.id, tar);
        }

        List<Place> places = res.getPossiblePlaces();
        log.debug(String.format("According to transitions, the state [%s] in chain [%s] could run on %s places", s.represents.getName(),
                s.chain.getName(), places.size()));

        // Check calendar
        for (Place p : places.toArray(new Place[0]))
        {
            if (!s.canRunAccordingToCalendarOnPlace(conn, p))
            {
                places.remove(p);
            }
        }
        log.debug(String.format("After taking calendar conditions into account, the state [%s] in chain [%s] could run on %s places",
                s.represents.getName(), s.chain.getName(), places.size()));

        // Go
        if (!places.isEmpty())
        {
            log.debug(String
                    .format("State (%s - chain %s) is triggered by the event on %s of its places. Analysis has consumed %s events on these places.",
                            s.represents.getName(), s.chain.getName(), places.size(), res.consumedEvents.size()));

            s.consumeEvents(res.consumedEvents, places, conn);
            for (Place p : places)
            {
                if (p.node.getComputingNode() == s.application.getLocalNode())
                {
                    s.runFromEngine(p, conn, pjProducer, session, evt);
                }
            }
            return res;
        }
        else
        {
            return new EventAnalysisResult(s);
        }
    }

    //
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // Methods called before and after run
    // Run - phase 1
    // Responsible for parameters resolution.
    // Default implementation resolves all parameters. Should usually be called
    // by overloads.
    public void prepareRun(PipelineJob pj, RunnerManager sender, ChronixContext ctx)
    {
        for (Parameter p : this.parameters)
        {
            p.resolveValue(ctx, sender, pj);
        }
    }

    // ?
    public void endOfRun(PipelineJob pj, RunnerManager sender, ChronixContext ctx, Connection conn)
    {
        log.info("end of run");
    }

    // Called before external run (i.e. sending the PJ to the runner agent)
    // Supposed to do local operations only.
    // Used by active nodes which influence the scheduling itself rather than run a payload.
    // Not called within an open JPA transaction - if you open one, close it!
    public void internalRun(Connection conn, ChronixContext ctx, PipelineJob pj, MessageProducer jmsProducer, Session jmsSession, DateTime virtualTime)
    {
        // Do nothing by default.
    }

    public DateTime selfTrigger(MessageProducer eventProducer, Session jmsSession, ChronixContext ctx, Connection conn, DateTime present)
            throws ChronixRunException
    {
        throw new NotImplementedException();
    }

    public RunResult forceOK()
    {
        RunResult rr = new RunResult();
        rr.returnCode = 0;
        rr.conditionData2 = null;
        rr.conditionData3 = null;
        rr.conditionData4 = null;
        rr.end = DateTime.now();
        rr.logStart = "Job forced OK";
        rr.fullerLog = rr.logStart;
        rr.start = rr.end;

        return rr;
    }

    //
    // ////////////////////////////////////////////////////////////////////////////
    // ////////////////////////////////////////////////////////////////////////////
    // Flags (engine and runner)
    // How should the runner agent run this source? (shell command, sql through
    // JDBC, ...)
    public String getActivityMethod()
    {
        return "None";
    }

    public String getSubActivityMethod()
    {
        return "None";
    }

    // Should it be run by a runner agent?
    public boolean hasExternalPayload()
    {
        return false;
    }

    // Should it be run locally but asynchronously?
    public boolean hasInternalPayload()
    {
        return false;
    }

    // Should the node execution results be visible in the history table?
    public boolean visibleInHistory()
    {
        return true;
    }

    // Should it be executed by the self-trigger agent?
    public boolean selfTriggered()
    {
        return false;
    }

    // How should it behave when multiple transition point on a single State?
    public MultipleTransitionsHandlingMode multipleTransitionHandling()
    {
        return MultipleTransitionsHandlingMode.AND;
    }
    //
    // ////////////////////////////////////////////////////////////////////////////
}
