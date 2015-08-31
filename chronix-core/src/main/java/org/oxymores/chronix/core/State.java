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
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.slf4j.Logger;
import org.hibernate.validator.constraints.Range;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.EnvironmentValue;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class State extends ApplicationObject
{
    private static final Logger log = LoggerFactory.getLogger(State.class);
    private static final long serialVersionUID = -2640644872229489081L;

    // /////////////////////////////////////////////////////////////////////////////////
    // Fields
    @NotNull
    protected Boolean parallel = false;

    // GUI data
    @Range(min = 0, max = 100000)
    @NotNull
    protected Integer x, y;

    // Time limits
    @Range(min = 0)
    protected Integer warnAfterMn, killAfterMn, maxPipeWaitTime, eventValidityMn;

    // The active element represented by this State
    @NotNull(message = "a state must be represent one event source - it currently has none")
    protected ActiveNodeBase represents;

    // The chain it belongs to
    @NotNull
    protected Chain chain;

    // Transitions
    @NotNull
    @Size(min = 0)
    @Valid
    protected List<Transition> trFromHere, trReceivedHere;

    // Exclusive states
    @NotNull
    protected List<State> exclusiveStates;

    // Runs on a place group
    @NotNull(message = "a state must run on exactly one place group - it currently has none")
    protected PlaceGroup runsOn;

    // Sequences
    protected List<AutoSequence> sequences;
    protected List<Token> tokens;
    protected Calendar calendar;
    protected Boolean loopMissedOccurrences = false;
    protected Boolean endOfOccurrence = false;
    protected Boolean blockIfPreviousFailed = false;
    protected int calendarShift = 0;

    // Fields
    // /////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////
    // Construction / destruction
    public State()
    {
        super();
        this.exclusiveStates = new ArrayList<>();
        this.trFromHere = new ArrayList<>();
        this.trReceivedHere = new ArrayList<>();
        this.sequences = new ArrayList<>();
        this.tokens = new ArrayList<>();
    }

    //
    // /////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////
    // Stupid GET/SET
    public int getCalendarShift()
    {
        return calendarShift;
    }

    public void setCalendarShift(int shift)
    {
        this.calendarShift = shift;
    }

    public Boolean getLoopMissedOccurrences()
    {
        return loopMissedOccurrences;
    }

    public void setLoopMissedOccurrences(Boolean loopMissedOccurrences)
    {
        this.loopMissedOccurrences = loopMissedOccurrences;
    }

    public Boolean getEndOfOccurrence()
    {
        return endOfOccurrence;
    }

    public void setEndOfOccurrence(Boolean endOfOccurrence)
    {
        this.endOfOccurrence = endOfOccurrence;
    }

    public PlaceGroup getRunsOn()
    {
        return this.runsOn;
    }

    public List<Place> getRunsOnPlaces()
    {
        return this.runsOn.getPlaces();
    }

    public void setRunsOn(PlaceGroup group)
    {
        this.runsOn = group;
    }

    public List<Transition> getTrFromHere()
    {
        return trFromHere;
    }

    public List<Transition> getTrReceivedHere()
    {
        return trReceivedHere;
    }

    public List<AutoSequence> getSequences()
    {
        return sequences;
    }

    public Calendar getCalendar()
    {
        return calendar;
    }

    public Boolean getParallel()
    {
        return parallel;
    }

    public void setParallel(Boolean parallel)
    {
        this.parallel = parallel;
    }

    public Integer getX()
    {
        return x;
    }

    public void setX(Integer x)
    {
        this.x = x;
    }

    public Integer getY()
    {
        return y;
    }

    public void setY(Integer y)
    {
        this.y = y;
    }

    public Integer getWarnAfterMn()
    {
        return warnAfterMn;
    }

    public void setWarnAfterMn(Integer warnAfterMn)
    {
        this.warnAfterMn = warnAfterMn;
    }

    public Integer getKillAfterMn()
    {
        return killAfterMn;
    }

    public void setKillAfterMn(Integer killAfterMn)
    {
        this.killAfterMn = killAfterMn;
    }

    public Integer getMaxPipeWaitTime()
    {
        return maxPipeWaitTime;
    }

    public void setMaxPipeWaitTime(Integer maxPipeWaitTime)
    {
        this.maxPipeWaitTime = maxPipeWaitTime;
    }

    public Integer getEventValidityMn()
    {
        return eventValidityMn;
    }

    public void setEventValidityMn(Integer eventValidityMn)
    {
        this.eventValidityMn = eventValidityMn;
    }

    public ActiveNodeBase getRepresents()
    {
        return represents;
    }

    public void setRepresents(ActiveNodeBase represents)
    {
        this.represents = represents;
    }

    public Chain getChain()
    {
        return chain;
    }

    public void setChain(Chain chain)
    {
        this.chain = chain;

        if (chain != null)
        {
            this.application = chain.getApplication();
            chain.addState(this);
        }
    }

    public List<State> getExclusiveStates()
    {
        return exclusiveStates;
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

    public void setCalendar(Calendar c)
    {
        c.s_addStateUsing(this);
        this.calendar = c;
    }

    public void removeCalendar()
    {
        if (this.calendar != null)
        {
            this.calendar.s_removeStateUsing(this);
            this.calendar = null;
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
        for (Transition t : trFromHere)
        {
            res.add(t.stateTo);
        }
        return res;
    }

    public List<State> getParentStates()
    {
        ArrayList<State> res = new ArrayList<>();
        for (Transition t : trReceivedHere)
        {
            res.add(t.stateFrom);
        }
        return res;
    }

    public void addTransitionFromHere(Transition tr)
    {
        if (!this.trFromHere.contains(tr))
        {
            this.trFromHere.add(tr);
            tr.setStateFrom(this);
        }
    }

    public void addTransitionReceivedHere(Transition tr)
    {
        if (!this.trReceivedHere.contains(tr))
        {
            this.trReceivedHere.add(tr);
            tr.setStateTo(this);
        }
    }

    public Transition connectTo(State target)
    {
        return connectTo(target, 0, null, null, null, false);
    }

    public Transition connectTo(State target, Boolean calendarAware)
    {
        return connectTo(target, 0, null, null, null, calendarAware);
    }

    public Transition connectTo(State target, Integer guard1)
    {
        return connectTo(target, guard1, null, null, null, false);
    }

    public Transition connectTo(State target, Integer guard1, String guard2, String guard3, UUID guard4, Boolean calendarAware)
    {
        // Note: there can be multiple transitions between two states.
        Transition t = new Transition();
        t.setStateFrom(this);
        t.setStateTo(target);
        t.setGuard1(guard1);
        t.setGuard2(guard2);
        t.setGuard3(guard3);
        t.setGuard4(guard4);
        t.setCalendarAware(calendarAware);
        t.setApplication(this.application);
        this.chain.addTransition(t);
        return t;
    }

    public List<ExecutionNode> getRunsOnExecutionNodes()
    {
        ArrayList<ExecutionNode> res = new ArrayList<>();
        for (Place p : this.runsOn.getPlaces())
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
                if (!res.contains(n.getHost()))
                {
                    res.add(n.getHost());
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
    // /////////////////////////////////////////////////////////////////////////////////
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

    // /////////////////////////////////////////////////////////////////////////////////
    // Create and post PJ for run
    /**
     To be called to launch a state without any consequences whatsoever: it will not trigger the launch of other states, it will not
     update calendars. Used to test launch a single state. Virtual time used is system current time.
     @param p
     @param pjProducer
     @param session
     */
    public void runAlone(Place p, MessageProducer pjProducer, Session session)
    {
        run(p, pjProducer, session, null, false, true, true, this.chain.id, UUID.randomUUID(), null, null, null, DateTime.now());
    }

    /**
     To be called to launch a state at once that will normally trigger all what should happen after it, but without
     updating calendars, i.e. without having any consequences on the normal scheduled plan. Mostly used to test launch a chain.<br/>
     Will create a PipelineJob with:
     <ul>
     <li>No calendar pointer to update</li>
     <li>inside plan, inside chain (level0 id = chain id)</li>
     <li>with a new chain luanch ID (level1 id)</li>
     <li>no env values</li>
     </ul>
     @param p the Place on which to run the state.
     @param pjProducer a JMS MessageProducer that will send the newly created PiplelineJob
     @param session a JMS session (used in conjunction with pjProducer)
     @param virtualTime
     */
    public void runInsidePlanWithoutUpdatingCalendar(Place p, MessageProducer pjProducer, Session session, DateTime virtualTime)
    {
        run(p, pjProducer, session, null, false, false, false, this.chain.id, UUID.randomUUID(), null, null, null, virtualTime);
    }

    /**
     Runs a state on all its places. Will have full consequences, including updating calendars. So if called from outside the engine it may
     very well botch the standard scheduled plan!
     @param conn
     @param pjProducer
     @param jmsSession
     @param level2Id
     @param level3Id
     @param virtualTime
     */
    public void runInsidePlan(Connection conn, MessageProducer pjProducer, Session jmsSession, UUID level2Id, UUID level3Id, DateTime virtualTime)
    {
        for (Place p : this.runsOn.places)
        {
            runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), level2Id, level3Id, virtualTime);
        }
    }

    public void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, DateTime virtualTime)
    {
        runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), null, null, virtualTime);
    }

    private void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, UUID level1Id, UUID level2Id, UUID level3Id,
            DateTime virtualTime)
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

        run(p, pjProducer, jmsSession, calendarOccurrenceID, true, false, false, this.chain.id, level1Id, level2Id, level3Id, cpToUpdate, virtualTime);
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

        run(p, pjProducer, session, calendarOccurrenceID, true, false, e.getOutsideChain(), e.getLevel0Id(), e.getLevel1Id(), e.getLevel2Id(), e.getLevel3Id(), cpToUpdate,
                e.getVirtualTime(), e.getEnvValues(conn).toArray(new EnvironmentValue[0]));
    }

    private void run(Place p, MessageProducer pjProducer, Session session, UUID calendarOccurrenceID, boolean updateCalendarPointer, boolean outOfPlan, boolean outOfChainLaunch,
            UUID level0Id, UUID level1Id, UUID level2Id, UUID level3Id, CalendarPointer cpToUpdate, DateTime virtualTime, EnvironmentValue... params)
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
        if (this.warnAfterMn != null)
        {
            pj.setWarnNotEndedAt(now.plusMinutes(this.warnAfterMn));
        }
        else
        {
            pj.setWarnNotEndedAt(now.plusDays(1));
        }

        if (this.killAfterMn != null)
        {
            pj.setKillAt(now.plusMinutes(this.killAfterMn));
        }

        // Environment variables from the State itself
        for (EnvironmentParameter ep : this.envParams)
        {
            pj.addEnvValueToCache(ep.key, ep.value);
        }

        // Environment variables passed from other jobs through the event (or manually set)
        for (EnvironmentValue ep : params)
        {
            if (ep.getKey().startsWith("CHR_"))
            {
                // Don't propagate auto variables
                continue;
            }
            pj.addEnvValueToCache(ep.getKey(), ep.getValue());
        }

        // Environment variables auto
        pj.addEnvValueToCache("CHR_STATEID", this.id.toString());
        pj.addEnvValueToCache("CHR_CHAINID", this.chain.id.toString());
        pj.addEnvValueToCache("CHR_LAUNCHID", pj.getId().toString());
        pj.addEnvValueToCache("CHR_JOBNAME", this.represents.name);
        pj.addEnvValueToCache("CHR_PLACEID", p.id.toString());
        pj.addEnvValueToCache("CHR_PLACENAME", p.name);
        pj.addEnvValueToCache("CHR_PLACEGROUPID", this.runsOn.id.toString());
        pj.addEnvValueToCache("CHR_PLACEGROUPNAME", this.runsOn.name);

        // Calendar update
        if (this.usesCalendar())
        {
            pj.setCalendarOccurrenceID(calendarOccurrenceID);
            pj.setCalendar(calendar);

            log.debug("Since this state will run, calendar update!");
            cpToUpdate.setRunning(true);
            if (updateCalendarPointer)
            {
                cpToUpdate.setLastStartedOccurrenceId(cpToUpdate.getNextRunOccurrenceId());
            }

            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CALENDAR, this.calendar.name);
            pj.addEnvValueToCache(Constants.ENV_AUTO_CHR_CHR_CALENDARID, this.calendar.id.toString());
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
            SenderHelpers.sendToPipeline(pj, p.getNode().getHost(), pjProducer, session, false);
        }
        catch (JMSException e1)
        {
            log.error("Could not enqueue a state for launch. Scheduler will still go on, but this may affect its operations.", e1);
        }

        // Done
        log.debug(String.format("State (%s - chain %s) was enqueued for launch on place %s", this.represents.getName(), this.chain.getName(), p.name));
    }

    // Create and post PJ for run
    // /////////////////////////////////////////////////////////////////////////////////
    // /////////////////////////////////////////////////////////////////////////////////
    // Calendar stuff
    // Called within an open transaction. Won't be committed here.
    public void createPointers(Connection conn)
    {
        if (!this.usesCalendar())
        {
            return;
        }

        // Get existing pointers
        List<CalendarPointer> ptrs = conn.createQuery("SELECT * FROM CalendarPointer p WHERE p.stateID = :stateID").
                addParameter("stateID", this.id).executeAndFetch(CalendarPointer.class);

        // A pointer should exist on all places
        int i = 0;
        for (Place p : this.runsOn.places)
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
                CalendarDay cd = this.calendar.getCurrentOccurrence(conn);
                CalendarDay cdLast = this.calendar.getOccurrenceShiftedBy(cd, this.calendarShift - 1);
                CalendarDay cdNext = this.calendar.getOccurrenceShiftedBy(cd, this.calendarShift);
                CalendarPointer tmp = new CalendarPointer();
                tmp.setApplication(this.application);
                tmp.setCalendar(this.calendar);
                tmp.setLastEndedOkOccurrenceCd(cdLast);
                tmp.setLastEndedOccurrenceCd(cdLast);
                tmp.setLastStartedOccurrenceCd(cdLast);
                tmp.setNextRunOccurrenceCd(cdNext);
                tmp.setPlace(p);
                tmp.setApplication(this.calendar.getApplication());
                tmp.setState(this);
                i++;

                tmp.insertOrUpdate(conn);
            }
        }

        if (i != 0)
        {
            log.debug(String.format("State %s (%s - chain %s) has created %s calendar pointer(s).", this.id, this.represents.name, this.chain.name, i));
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

        log.debug(String.format("State %s (%s - chain %s) uses a calendar. Calendar analysis begins.", this.id, this.represents.name, this.chain.name));

        // Get the pointer (on the given place)
        /*conn.createQuery("SELECT e.* FROM CalendarPointer p WHERE p.stateID = :stateID AND p.placeID = :placeID").
         addParameter("stateID", this.id).addParameter("placeID", p.getId())
         Query q = em.createQuery();
         q.setParameter(1, this.id.toString());
         q.setParameter(2, p.getId().toString());
         */
        CalendarPointer cp = this.getCurrentCalendarPointer(conn, p);
        if (cp == null)
        {
            log.error(String.format("State %s (%s - chain %s): CalendarPointer is null - should not be possible. It's a bug.", this.id, this.represents.name, this.chain.name));
            return false;
        }

        CalendarDay nextRunOccurrence = this.calendar.getDay(cp.getNextRunOccurrenceId());
        if (nextRunOccurrence == null)
        {
            log.error(String.format("There is no next occurrence for calendar %s. Please add some.", this.calendar.getName()));
            return false;
        }

        // Only one occurrence can run at the same time
        if (cp.getRunning())
        {
            log.debug("Previous run has not ended - it must end for a new run to occur");
            return false;
        }

        // Only run if previous run was OK (unless asked for)
        if (cp.getLatestFailed() && this.blockIfPreviousFailed)
        {
            log.debug("Previous run has ended incorrectly - it must end correctly for a new run to occur");
            return false;
        }

        // Sequence must be respected
        // But actually, nothing has to be done to enforce it as it comes from either the scheduler itself or the user.
        // No further than the calendar itself
        CalendarDay baseLimit = this.calendar.getCurrentOccurrence(conn);

        log.debug(String.format("Calendar limit is currently: %s. Shift is %s, next occurrence to run for this state is %s", baseLimit.seq, this.calendarShift,
                nextRunOccurrence.seq));
        CalendarDay shiftedLimit = this.calendar.getOccurrenceShiftedBy(baseLimit, this.calendarShift);

        // Shift: -1 means that the State will run at D-1 when the reference is
        // D. Therefore it should stop one occurrence before the others.
        if (!this.calendar.isBeforeOrSame(nextRunOccurrence, shiftedLimit))
        {
            log.debug(String.format("This is too soon to launch the job: calendar is at %s (with shift , this limit becomes %s), while this state wants to already run %s",
                    baseLimit.seq, shiftedLimit.seq, nextRunOccurrence.seq));
            return false;
        }

        // If here, alles gut.
        log.debug(String.format("State %s (%s - chain %s) can run according to its calendar.", this.id, this.represents.name, this.chain.name));
        return true;
    }

    public boolean isLate(Connection conn, Place p)
    {
        // LATE MEANS: State Latest OK < Calendar current + State shift
        CalendarDay cd = this.getCurrentCalendarOccurrence(conn, p);
        CalendarDay limit = this.calendar.getOccurrenceShiftedBy(this.calendar.getCurrentOccurrence(conn), this.calendarShift);

        return this.calendar.isBefore(cd, limit);
    }

    public Boolean usesCalendar()
    {
        return this.calendar != null;
    }

    public CalendarDay getCurrentCalendarOccurrence(Connection conn, Place p)
    {
        return this.calendar.getDay(this.getCurrentCalendarPointer(conn, p).getLastEndedOkOccurrenceUuid());
    }

    public CalendarPointer getCurrentCalendarPointer(Connection conn, Place p)
    {
        if (!usesCalendar())
        {
            // We will get NullPointerException, but calls to the function should never be done without checking the need of a calendar
            return null;
        }

        return conn.createQuery("SELECT * FROM CalendarPointer p WHERE p.stateID = :stateID AND p.placeID = :placeID AND p.calendarID = :calendarID")
                .addParameter("stateID", this.id).addParameter("placeID", p.id).addParameter("calendarID", this.calendar.id).executeAndFetchFirst(CalendarPointer.class);
    }

    // Calendar stuff
    // ///////////////////////////////////////////////////////////////////////////////
}
