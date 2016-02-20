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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.constraints.NotNull;

import org.oxymores.chronix.core.context.Application2;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.EventSourceOptionInvisible;
import org.oxymores.chronix.core.source.api.EventSourceOptionSelfTriggered;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.ChronixEngine;
import org.oxymores.chronix.engine.RunnerManager;
import org.oxymores.chronix.engine.data.EventAnalysisResult;
import org.oxymores.chronix.engine.data.PlaceAnalysisResult;
import org.oxymores.chronix.engine.data.TransitionAnalysisResult;
import org.oxymores.chronix.engine.modularity.runner.RunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

public class EventSourceWrapper implements Serializable
{
    private static final long serialVersionUID = 2317281646089939267L;
    private static final Logger log = LoggerFactory.getLogger(EventSourceWrapper.class);

    @NotNull
    protected ArrayList<Parameter> parameters;

    private Application2 application;

    // A simple indication - only used when a plugin is missing and we need its name to help the user.
    private String pluginName;

    // The real event source description. Must NOT be XML-serialised. Each plugin is responsible for its own serialisation.
    @XStreamOmitField
    private EventSource eventSource;

    private boolean enabled = true;

    public EventSourceWrapper(Application2 app, EventSource source, String pluginSymbolicName)
    {
        super();
        this.application = app;
        this.eventSource = source;
        this.pluginName = pluginSymbolicName;
        parameters = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s)", eventSource.getName(), pluginName);
    }

    ///////////////////////////////////////////////////////////////////////////
    // stupid get/set

    public String getName()
    {
        return this.eventSource.getName();
    }

    public UUID getId()
    {
        return this.eventSource.getId();
    }

    public String getSourceClass()
    {
        return this.eventSource.getClass().getCanonicalName();
    }

    public String getPluginSymbolicName()
    {
        return this.pluginName;
    }

    public EventSource getSource()
    {
        return this.eventSource;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public boolean isContainer()
    {
        return this.eventSource instanceof EventSourceContainer;
    }

    // stupid get/set
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Relationship traversing
    public List<State> getClientStates()
    {
        return this.application.getStatesClientOfSource(getId());
    }

    // Relationship traversing
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
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
        addParameter(p);
    }

    // Parameter handling
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Event analysis

    // Do the given events allow for a transition originating from a state representing this source?
    public PlaceAnalysisResult createdEventRespectsTransitionOnPlace(DTOTransition tr, List<Event> events, Place p, Connection conn)
    {
        PlaceAnalysisResult res = new PlaceAnalysisResult(p);
        res.res = false;

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
            if (!this.eventSource.isTransitionPossible(tr, e))
            {
                continue;
            }

            // Check calendar if the transition is calendar-aware
            if (tr.isCalendarAware())
            {
                State stateTo = this.application.getState(tr.getTo());
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
            ChronixEngine engine)
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
            for (Place p : s.getRunsOnPlaces())
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
        for (DTOTransition tr : s.getTransitionsReceivedHere())
        {
            log.debug(String.format("State %s (%s - chain %s) analysis with %s events", s.getId(), s.getRepresents().getName(),
                    s.getContainerName(), sessionEvents.size()));

            TransitionAnalysisResult tar = State.isTransitionAllowed(this.application, tr, sessionEvents, conn);

            if (tar.totallyBlocking()) // we do an AND by default
            {
                // No need to go further - one transition will block everything
                log.debug(String.format("State %s (%s - chain %s) is NOT allowed to run due to transition from %s", s.getId(),
                        s.getRepresents().getName(), s.getContainerName(),
                        this.application.getState(tr.getFrom()).getRepresents().getName()));
                return new EventAnalysisResult(s);
            }

            res.analysis.put(tr.getId(), tar);
        }

        List<Place> places = res.getPossiblePlaces();
        log.debug(String.format("According to transitions, the state [%s] in chain [%s] could run on %s places",
                s.getRepresents().getName(), s.getContainerName(), places.size()));

        // Check calendar
        for (Place p : places.toArray(new Place[0]))
        {
            if (!s.canRunAccordingToCalendarOnPlace(conn, p))
            {
                places.remove(p);
            }
        }
        log.debug(String.format("After taking calendar conditions into account, the state [%s] in chain [%s] could run on %s places",
                s.getRepresents().getName(), s.getContainerName(), places.size()));

        // Go
        if (!places.isEmpty())
        {
            log.debug(String.format(
                    "State (%s - chain %s) is triggered by the event on %s of its places. Analysis has consumed %s events on these places.",
                    s.getRepresents().getName(), s.getContainerName(), places.size(), res.consumedEvents.size()));

            s.consumeEvents(res.consumedEvents, places, conn);
            for (Place p : places)
            {
                if (p.node.getComputingNode() == engine.getLocalNode())
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
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Methods called before and after run
    // Run - phase 1
    // Responsible for parameters resolution.
    // Default implementation resolves all parameters. Should usually be called
    // by overloads.
    public void prepareRun(PipelineJob pj, RunnerManager sender)
    {
        /*
         * for (Parameter p : this.parameters) { p.resolveValue(ctx, sender, pj); }
         */
    }

    public RunResult run(EngineCallback cb, JobDescription jd)
    {
        EventSourceRunResult esrr = this.eventSource.run(cb, jd);
        if (esrr != null)
        {
            return new RunResult(jd, esrr);
        }
        return null;
    }

    public RunResult forceOK(EngineCallback cb, JobDescription jd)
    {
        return new RunResult(jd, this.eventSource.runForceOk(cb, jd));
    }

    public RunResult runDisabled(EngineCallback cb, JobDescription jd)
    {
        return new RunResult(jd, this.eventSource.runDisabled(cb, jd));
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Toggles

    public boolean isSelfTriggered()
    {
        return this.eventSource instanceof EventSourceOptionSelfTriggered;
    }

    public boolean isHiddenFromHistory()
    {
        return this.eventSource instanceof EventSourceOptionInvisible;
    }

    //
    ///////////////////////////////////////////////////////////////////////////

}
