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
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.joda.time.DateTime;
import org.oxymores.chronix.core.context.Application2;
import org.oxymores.chronix.core.source.api.EventSource;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.DTOState;
import org.oxymores.chronix.core.source.api.DTOTransition;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.EnvironmentValue;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.data.PlaceAnalysisResult;
import org.oxymores.chronix.engine.data.TransitionAnalysisResult;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class State extends ApplicationObject
{
    private static final Logger log = LoggerFactory.getLogger(State.class);
    private static final long serialVersionUID = -2640644872229489081L;

    // /////////////////////////////////////////////////////////////////////////////////
    // Fields

    // The data bag
    protected DTOState dto;

    // The container it belongs to
    @NotNull
    protected EventSourceContainer container;

    // Transitions
    @NotNull
    @Size(min = 0)
    @Valid
    protected List<DTOTransition> trFromHere, trReceivedHere;

    // TODO: Sequences (should be put inside DTO if possible)
    protected List<AutoSequence> sequences;
    protected List<Token> tokens;

    // Fields
    // /////////////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////////////
    // Construction / destruction
    public State(Application2 app, DTOState state, EventSourceContainer container, List<DTOTransition> trFromState, List<DTOTransition> trToState)
    {
        super();
        this.application = app;
        this.dto = state;
        this.container = container;

        this.trFromHere = new ArrayList<>(trFromState);
        this.trReceivedHere = new ArrayList<>(trToState);

        this.sequences = new ArrayList<>();
        this.tokens = new ArrayList<>();

        this.id = state.getId();
    }

    //
    // /////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////
    // Stupid GET/SET

    public PlaceGroup getRunsOn()
    {
        return this.application.getGroup(this.dto.getRunsOnId());
    }

    public String getContainerName()
    {
        if (this.container != null)
        {
            return this.container.getName();
        }
        return "first level launch";
    }

    public Calendar getCalendar()
    {
        return this.application.getCalendar(this.dto.getCalendarId());
    }

    public int getCalendarShift()
    {
        return this.dto.getCalendarShift();
    }

    public void addSequence(AutoSequence s)
    {
        s.s_addStateUsing(this);
        this.sequences.add(s);
    }

    public void removeSequence(AutoSequence s)
    {
        try
        {
            this.sequences.remove(s);
        }
        finally
        {
            s.s_removeStateUsing(this);
        }
    }

    public void addToken(Token t)
    {
        t.s_addStateUsing(this);
        this.tokens.add(t);
    }

    public void removeToken(Token t)
    {
        try
        {
            this.tokens.remove(t);
        }
        finally
        {
            t.s_removeStateUsing(this);
        }
    }

    public List<Token> getTokens()
    {
        return tokens;
    }

    public void setTokens(List<Token> tokens)
    {
        this.tokens = tokens;
    }

    // Stupid GET/SET
    // /////////////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////////////
    // Relationship handling
    public List<State> getClientStates()
    {
        ArrayList<State> res = new ArrayList<>();
        for (DTOTransition t : trFromHere)
        {
            res.add(this.application.getState(t.getTo()));
        }
        return res;
    }

    public List<State> getParentStates()
    {
        ArrayList<State> res = new ArrayList<>();
        for (DTOTransition t : trReceivedHere)
        {
            res.add(this.application.getState(t.getFrom()));
        }
        return res;
    }

    public List<DTOTransition> getTransitionsReceivedHere()
    {
        return this.trReceivedHere;
    }

    public EventSource getRepresents()
    {
        return this.application.getEventSource(this.dto.getEventSourceId());
    }

    public EventSourceWrapper getRepresentsContainer()
    {
        return this.application.getEventSourceContainer(this.dto.getEventSourceId());
    }

    /*
     * public Transition connectTo(State target, Integer guard1, String guard2, String guard3, UUID guard4, Boolean calendarAware) { //
     * Note: there can be multiple transitions between two states. Transition t = new Transition(); t.setStateFrom(this);
     * t.setStateTo(target); t.setGuard1(guard1); t.setGuard2(guard2); t.setGuard3(guard3); t.setGuard4(guard4);
     * t.setCalendarAware(calendarAware); t.setApplication(this.application); this.chain.addTransition(t); return t; }
     */

    public List<Place> getRunsOnPlaces()
    {
        return this.getRunsOn().getPlaces();
    }

    public List<ExecutionNode> getRunsOnExecutionNodes()
    {
        ArrayList<ExecutionNode> res = new ArrayList<>();
        for (Place p : this.getRunsOnPlaces())
        {
            if (!res.contains(p.getNode()))
            {
                res.add(p.getNode());
            }
        }
        return res;
    }

    public List<ExecutionNode> getRunsOnPhysicalNodes()
    {
        List<ExecutionNode> all = getRunsOnExecutionNodes();
        List<ExecutionNode> res = getRunsOnExecutionNodes();
        for (ExecutionNode n : all)
        {
            if (n.isHosted())
            {
                if (!res.contains(n.getComputingNode()))
                {
                    res.add(n.getComputingNode());
                }
            }
            else
            {
                // Not hosted - true Physical Node
                if (!res.contains(n))
                {
                    res.add(n);
                }
            }
        }
        return res;
    }

    // Relationship handling
    /////////////////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////////////
    // EVENT ANALYSIS

    public void consumeEvents(List<Event> events, List<Place> places, Connection conn)
    {
        for (Event e : events)
        {
            for (Place p : places)
            {
                log.debug(String.format("Event %s marked as consumed on place %s", e.getId(), p.name));
                EventConsumption ec = new EventConsumption();
                ec.setEventID(e.getId());
                ec.setPlaceID(p.getId());
                ec.setStateID(this.getId());
                ec.setAppID(e.getAppID());

                ec.insert(conn);
            }
        }
    }

    public static TransitionAnalysisResult isTransitionAllowed(Application2 app, DTOTransition tr, List<Event> events, Connection conn)
    {
        boolean parallelAnalaysis = isTransitionParallelEnabled(app, tr);
        TransitionAnalysisResult res = new TransitionAnalysisResult(tr, parallelAnalaysis);
        State from = app.getState(tr.getFrom());
        State to = app.getState(tr.getTo());

        for (Place p : from.getRunsOnPlaces())
        {
            ArrayList<Event> virginEvents = new ArrayList<>();
            for (Event e : events)
            {
                if (!e.wasConsumedOnPlace(p, to, conn) && !virginEvents.contains(e))
                {
                    virginEvents.add(e);
                }
            }
            res.addPlaceAnalysis(from.getRepresentsContainer().createdEventRespectsTransitionOnPlace(tr, virginEvents, p, conn));

            if (!parallelAnalaysis && !res.allowedOnAllPlaces())
            {
                // We already know the transition is KO, so no need to continue. Create a blocking analysis result for that.
                res = new TransitionAnalysisResult(tr, parallelAnalaysis);
                for (Place pp : from.getRunsOnPlaces())
                {
                    // Create a FORBIDDEN result on all places
                    res.addPlaceAnalysis(new PlaceAnalysisResult(pp));
                }
                return res;
            }
        }
        return res;
    }

    private static Boolean isTransitionParallelEnabled(Application2 app, DTOTransition tr)
    {
        State from = app.getState(tr.getFrom());
        State to = app.getState(tr.getTo());

        if (!from.dto.getParallel() || !to.dto.getParallel())
        {
            return false;
        }

        if (!from.getRunsOn().equals(to.getRunsOn()))
        {
            return false;
        }

        return true;
    }

    //
    ///////////////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////////////
    // Create and post PJ for run
    /**
     * To be called to launch a state without any consequences whatsoever: it will not trigger the launch of other states, it will not
     * update calendars. Used to test launch a single state. Virtual time used is system current time.
     * 
     * @param p
     * @param pjProducer
     * @param session
     */
    public void runAlone(Place p, MessageProducer pjProducer, Session session)
    {
        run(p, pjProducer, session, null, false, true, true, this.container.getId(), UUID.randomUUID(), null, null, null, DateTime.now());
    }

    /**
     * To be called to launch a state at once that will normally trigger all what should happen after it, but without updating calendars,
     * i.e. without having any consequences on the normal scheduled plan. Mostly used to test launch a chain.<br/>
     * Will create a PipelineJob with:
     * <ul>
     * <li>No calendar pointer to update</li>
     * <li>inside plan, inside chain (level0 id = chain id)</li>
     * <li>with a new chain luanch ID (level1 id)</li>
     * <li>no env values</li>
     * </ul>
     * 
     * @param p
     *            the Place on which to run the state.
     * @param pjProducer
     *            a JMS MessageProducer that will send the newly created PiplelineJob
     * @param session
     *            a JMS session (used in conjunction with pjProducer)
     * @param virtualTime
     */
    public void runInsidePlanWithoutUpdatingCalendar(Place p, MessageProducer pjProducer, Session session, DateTime virtualTime)
    {
        run(p, pjProducer, session, null, false, false, false, this.container.getId(), UUID.randomUUID(), null, null, null, virtualTime);
    }

    /**
     * Runs a state on all its places. Will have full consequences, including updating calendars. So if called from outside the engine it
     * may very well botch the standard scheduled plan!
     * 
     * @param conn
     * @param pjProducer
     * @param jmsSession
     * @param level2Id
     * @param level3Id
     * @param virtualTime
     */
    public void runInsidePlan(Connection conn, MessageProducer pjProducer, Session jmsSession, UUID level2Id, UUID level3Id,
            DateTime virtualTime)
    {
        for (Place p : this.getRunsOnPlaces())
        {
            runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), level2Id, level3Id, virtualTime);
        }
    }

    public void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, DateTime virtualTime)
    {
        runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), null, null, virtualTime);
    }

    private void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, UUID level1Id, UUID level2Id,
            UUID level3Id, DateTime virtualTime)
    {
        // Calendar update
        UUID calendarOccurrenceID = null;
        CalendarPointer cpToUpdate = null;
        if (this.usesCalendar())
        {
            CalendarPointer cp = this.getCurrentCalendarPointer(conn, p);
            calendarOccurrenceID = cp.getNextRunOccurrenceId();
            cpToUpdate = this.getCurrentCalendarPointer(conn, p);
        }

        run(p, pjProducer, jmsSession, calendarOccurrenceID, true, false, false, this.container.getId(), level1Id, level2Id, level3Id,
                cpToUpdate, virtualTime);
    }

    public void runFromEngine(Place p, Connection conn, MessageProducer pjProducer, Session session, Event e)
    {
        // Calendar update
        UUID calendarOccurrenceID = null;
        CalendarPointer cpToUpdate = null;
        if (this.usesCalendar())
        {
            CalendarPointer cp = this.getCurrentCalendarPointer(conn, p);
            calendarOccurrenceID = cp.getNextRunOccurrenceId();
            cpToUpdate = this.getCurrentCalendarPointer(conn, p);
        }

        run(p, pjProducer, session, calendarOccurrenceID, true, false, e.getOutsideChain(), e.getLevel0Id(), e.getLevel1Id(),
                e.getLevel2Id(), e.getLevel3Id(), cpToUpdate, e.getVirtualTime(), e.getEnvValues(conn).toArray(new EnvironmentValue[0]));
    }

    private void run(Place p, MessageProducer pjProducer, Session session, UUID calendarOccurrenceID, boolean updateCalendarPointer,
            boolean outOfPlan, boolean outOfChainLaunch, UUID level0Id, UUID level1Id, UUID level2Id, UUID level3Id,
            CalendarPointer cpToUpdate, DateTime virtualTime, EnvironmentValue... params)
    {
        DateTime now = DateTime.now();

        PipelineJob pj = new PipelineJob();

        // Common fields
        pj.setLevel0Id(level0Id);
        pj.setLevel1Id(level1Id);
        pj.setLevel2Id(level2Id);
        pj.setLevel3Id(level3Id);
        pj.setMarkedForRunAt(now);
        pj.setPlace(p);
        pj.setAppID(this.getApplication().getId());
        pj.setState(this);
        pj.setStatus("ENTERING_QUEUE");
        pj.setApplication(this.application);
        pj.setOutsideChain(outOfChainLaunch);
        pj.setIgnoreCalendarUpdating(!updateCalendarPointer);
        pj.setOutOfPlan(outOfPlan);
        pj.setVirtualTime(virtualTime);

        // Warning and kill
        if (this.dto.getWarnAfterMn() != null)
        {
            pj.setWarnNotEndedAt(now.plusMinutes(this.dto.getWarnAfterMn()));
        }
        else
        {
            pj.setWarnNotEndedAt(now.plusDays(1));
        }

        if (this.dto.getKillAfterMn() != null)
        {
            pj.setKillAt(now.plusMinutes(this.dto.getKillAfterMn()));
        }

        // Environment variables from the plan definition & runtime
        for (Map.Entry<String, String> ep : EnvironmentParameter.resolveRuntimeEnvironment(this, p, params).entrySet())
        {
            pj.addEnvValueToCache(ep.getKey(), ep.getValue());
        }

        // Environment variables auto
        pj.addEnvValueToCache("CHR_STATEID", this.id.toString());
        pj.addEnvValueToCache("CHR_CONTAINERID", this.container == null ? "" : this.container.getId().toString());
        pj.addEnvValueToCache("CHR_LAUNCHID", pj.getId().toString());
        pj.addEnvValueToCache("CHR_JOBNAME", this.getRepresents().getName());
        pj.addEnvValueToCache("CHR_PLACEID", p.id.toString());
        pj.addEnvValueToCache("CHR_PLACENAME", p.name);
        pj.addEnvValueToCache("CHR_PLACEGROUPID", this.getRunsOn().getId().toString());
        pj.addEnvValueToCache("CHR_PLACEGROUPNAME", this.getRunsOn().getName());

        // Calendar update
        if (this.usesCalendar())
        {
            Calendar c = this.application.getCalendar(this.dto.getCalendarId());
            pj.setCalendarOccurrenceID(calendarOccurrenceID);
            pj.setCalendar(c);

            log.debug("Since this state will run, calendar update!");
            cpToUpdate.setRunning(true);
            if (updateCalendarPointer)
            {
                cpToUpdate.setLastStartedOccurrenceId(cpToUpdate.getNextRunOccurrenceId());
            }

            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CALENDAR, c.getName());
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CHR_CALENDARID, c.getId().toString());
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CHR_CALENDARDATE, cpToUpdate.getNextRunOccurrenceId().toString());
        }
        else
        {
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CALENDAR, "NONE");
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CHR_CALENDARID, "NONE");
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CHR_CALENDARDATE, "NONE");
        }

        // Send it (commit is done by main engine later)
        try
        {
            SenderHelpers.sendToPipeline(pj, p.getNode().getComputingNode(), pjProducer, session, false);
        }
        catch (JMSException e1)
        {
            log.error("Could not enqueue a state for launch. Scheduler will still go on, but this may affect its operations.", e1);
        }

        // Done
        log.debug(String.format("State (%s - chain %s) was enqueued for launch on place %s", this.getRepresents().getName(),
                this.getContainerName(), p.name));
    }

    // Create and post PJ for run
    /////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////
    // Calendar stuff

    // Called within an open transaction. Won't be committed here.
    public void createPointers(Connection conn)
    {
        if (!this.usesCalendar())
        {
            return;
        }
        Calendar cal = this.getCalendar();

        // Get existing pointers
        List<CalendarPointer> ptrs = conn.createQuery("SELECT * FROM CalendarPointer p WHERE p.stateID = :stateID")
                .addParameter("stateID", this.id).executeAndFetch(CalendarPointer.class);

        // A pointer should exist on all places
        int i = 0;
        for (Place p : this.getRunsOnPlaces())
        {
            // Is there a existing pointer on this place?
            CalendarPointer existing = null;
            for (CalendarPointer retrieved : ptrs)
            {
                if (retrieved.getPlaceID().equals(p.id))
                {
                    existing = retrieved;
                    break;
                }
            }

            // If not, create one
            if (existing == null)
            {
                // A pointer should be created on this place!
                CalendarDay cd = cal.getCurrentOccurrence(conn);
                CalendarDay cdLast = cal.getOccurrenceShiftedBy(cd, this.dto.getCalendarShift() - 1);
                CalendarDay cdNext = cal.getOccurrenceShiftedBy(cd, this.dto.getCalendarShift());
                CalendarPointer tmp = new CalendarPointer();
                tmp.setApplication(this.application);
                tmp.setCalendar(cal);
                tmp.setLastEndedOkOccurrenceCd(cdLast);
                tmp.setLastEndedOccurrenceCd(cdLast);
                tmp.setLastStartedOccurrenceCd(cdLast);
                tmp.setNextRunOccurrenceCd(cdNext);
                tmp.setPlace(p);
                tmp.setApplication(this.application);
                tmp.setState(this);
                i++;

                tmp.insertOrUpdate(conn);
            }
        }

        if (i != 0)
        {
            log.debug(String.format("State %s (%s - chain %s) has created %s calendar pointer(s).", this.id, this.getRepresents().getName(),
                    this.getContainerName(), i));
        }
        // Commit is done by the calling method
    }

    public boolean canRunAccordingToCalendarOnPlace(Connection conn, Place p)
    {
        if (!this.usesCalendar())
        {
            // no calendar = no calendar constraints, just return true.
            log.debug("Does not use a calendar - crontab mode");
            return true;
        }
        Calendar cal = this.getCalendar();

        log.debug(String.format("State %s (%s - chain %s) uses a calendar. Calendar analysis begins.", this.id,
                this.getRepresents().getName(), this.getContainerName()));

        // Get the pointer (on the given place)
        /*
         * conn.createQuery("SELECT e.* FROM CalendarPointer p WHERE p.stateID = :stateID AND p.placeID = :placeID").
         * addParameter("stateID", this.id).addParameter("placeID", p.getId()) Query q = em.createQuery(); q.setParameter(1,
         * this.id.toString()); q.setParameter(2, p.getId().toString());
         */
        CalendarPointer cp = this.getCurrentCalendarPointer(conn, p);
        if (cp == null)
        {
            log.error(String.format("State %s (%s - chain %s): CalendarPointer is null - should not be possible. It's a bug.", this.id,
                    this.getRepresents().getName(), this.getContainerName()));
            return false;
        }

        CalendarDay nextRunOccurrence = cal.getDay(cp.getNextRunOccurrenceId());
        if (nextRunOccurrence == null)
        {
            log.error(String.format("There is no next occurrence for calendar %s. Please add some.", cal.getName()));
            return false;
        }

        // Only one occurrence can run at the same time
        if (cp.getRunning())
        {
            log.debug("Previous run has not ended - it must end for a new run to occur");
            return false;
        }

        // Only run if previous run was OK (unless asked for)
        if (cp.getLatestFailed() && this.dto.getBlockIfPreviousFailed())
        {
            log.debug("Previous run has ended incorrectly - it must end correctly for a new run to occur");
            return false;
        }

        // Sequence must be respected
        // But actually, nothing has to be done to enforce it as it comes from either the scheduler itself or the user.
        // No further than the calendar itself
        CalendarDay baseLimit = cal.getCurrentOccurrence(conn);

        log.debug(String.format("Calendar limit is currently: %s. Shift is %s, next occurrence to run for this state is %s", baseLimit.seq,
                this.dto.getCalendarShift(), nextRunOccurrence.seq));
        CalendarDay shiftedLimit = cal.getOccurrenceShiftedBy(baseLimit, this.dto.getCalendarShift());

        // Shift: -1 means that the State will run at D-1 when the reference is
        // D. Therefore it should stop one occurrence before the others.
        if (!cal.isBeforeOrSame(nextRunOccurrence, shiftedLimit))
        {
            log.debug(String.format(
                    "This is too soon to launch the job: calendar is at %s (with shift , this limit becomes %s), while this state wants to already run %s",
                    baseLimit.seq, shiftedLimit.seq, nextRunOccurrence.seq));
            return false;
        }

        // If here, alles gut.
        log.debug(String.format("State %s (%s - chain %s) can run according to its calendar.", this.id, this.getRepresents().getName(),
                this.getContainerName()));
        return true;
    }

    public boolean isLate(Connection conn, Place p)
    {
        // LATE MEANS: State Latest OK < Calendar current + State shift
        Calendar cal = this.getCalendar();
        CalendarDay cd = this.getCurrentCalendarOccurrence(conn, p);
        CalendarDay limit = cal.getOccurrenceShiftedBy(cal.getCurrentOccurrence(conn), this.dto.getCalendarShift());

        return cal.isBefore(cd, limit);
    }

    public Boolean usesCalendar()
    {
        return this.dto.getCalendarId() != null;
    }

    public CalendarDay getCurrentCalendarOccurrence(Connection conn, Place p)
    {
        return this.getCalendar().getDay(this.getCurrentCalendarPointer(conn, p).getLastEndedOkOccurrenceUuid());
    }

    public CalendarPointer getCurrentCalendarPointer(Connection conn, Place p)
    {
        if (!usesCalendar())
        {
            // We will get NullPointerException, but calls to the function should never be done without checking the need of a calendar
            return null;
        }

        return conn
                .createQuery(
                        "SELECT * FROM CalendarPointer p WHERE p.stateID = :stateID AND p.placeID = :placeID AND p.calendarID = :calendarID")
                .addParameter("stateID", this.id).addParameter("placeID", p.id).addParameter("calendarID", this.dto.getCalendarId())
                .executeAndFetchFirst(CalendarPointer.class);
    }

    // Calendar stuff
    // ///////////////////////////////////////////////////////////////////////////////
}
