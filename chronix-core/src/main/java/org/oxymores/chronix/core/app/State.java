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
package org.oxymores.chronix.core.app;

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
import org.oxymores.chronix.api.source.DTOState;
import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.core.ApplicationObject;
import org.oxymores.chronix.core.EnvironmentParameter;
import org.oxymores.chronix.core.engine.api.DTOToken;
import org.oxymores.chronix.core.network.ExecutionNode;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.EnvironmentValue;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.Constants;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

public class State extends ApplicationObject
{
    private static final Logger log = LoggerFactory.getLogger(State.class);
    private static final long serialVersionUID = -2640644872229489081L;

    ///////////////////////////////////////////////////////////////////////////
    // Fields

    // The data bag
    DTOState dto;

    // The container it belongs to
    @NotNull
    private EventSourceDef container;

    // Transitions
    @NotNull
    @Size(min = 0)
    @Valid
    private List<DTOTransition> trFromHere, trReceivedHere;

    // TODO: Sequences (should be put inside DTO if possible)
    private List<AutoSequence> sequences;

    // Fields
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Construction / destruction
    public State(Application app, DTOState state, EventSourceDef container, List<DTOTransition> trFromState, List<DTOTransition> trToState)
    {
        super();
        this.application = app;
        this.dto = state;
        this.container = container;

        this.trFromHere = new ArrayList<>(trFromState);
        this.trReceivedHere = new ArrayList<>(trToState);

        this.sequences = new ArrayList<>();

        this.id = state.getId();
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
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

    public FunctionalSequence getCalendar()
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

    public List<DTOToken> getTokens()
    {
        List<DTOToken> res = new ArrayList<>();
        for (UUID id : dto.getTokens())
        {
            res.add(application.getToken(id));
        }
        return res;
    }

    public boolean isParallel()
    {
        return this.dto.getParallel();
    }

    public boolean isCalendarUpdater()
    {
        return this.dto.isMoveCalendarForwardOnSuccess();
    }

    // Stupid GET/SET
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
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

    public EventSourceDef getEventSourceDefinition()
    {
        return this.application.getEventSource(this.dto.getEventSourceId());
    }

    public UUID getRepresentsId()
    {
        return this.dto.getEventSourceId();
    }

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
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
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
        run(p, pjProducer, session, null, false, true, true, this.container.getId(), UUID.randomUUID(), null, null, null, DateTime.now(),
                null);
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
        run(p, pjProducer, session, null, false, false, false, this.container.getId(), UUID.randomUUID(), null, null, null, virtualTime,
                null);
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
            DateTime virtualTime, Map<String, String> fieldOverload)
    {
        for (Place p : this.getRunsOnPlaces())
        {
            runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), level2Id, level3Id, virtualTime, fieldOverload);
        }
    }

    public void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, DateTime virtualTime,
            Map<String, String> fieldOverload)
    {
        runInsidePlan(p, conn, pjProducer, jmsSession, UUID.randomUUID(), null, null, virtualTime, fieldOverload);
    }

    private void runInsidePlan(Place p, Connection conn, MessageProducer pjProducer, Session jmsSession, UUID level1Id, UUID level2Id,
            UUID level3Id, DateTime virtualTime, Map<String, String> fieldOverload)
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
                cpToUpdate, virtualTime, fieldOverload);
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
                e.getLevel2Id(), e.getLevel3Id(), cpToUpdate, e.getVirtualTime(), null,
                e.getEnvValues(conn).toArray(new EnvironmentValue[0]));
    }

    /**
     * The one and only way of creating a PipelineJob inside the engine.
     * 
     * @param p
     *            the place on which the run should occur. It must be one of the places linked to this state.
     * @param pjProducer
     *            JMS
     * @param session
     *            JMS
     * @param calendarOccurrenceID
     * @param updateCalendarPointer
     *            used only if {@link #usesCalendar()}. Will advance the pointer associated to state/place if run ends OK.
     * @param outOfPlan
     *            will be created inside an isolated context. No consequences on calendar or other launches
     * @param outOfChainLaunch
     *            TODO: doc this.
     * @param level0Id
     * @param level1Id
     * @param level2Id
     * @param level3Id
     * @param cpToUpdate
     * @param virtualTime
     * @param fieldOverloads
     *            key/values that will overload the parameters of the source definition. These must be actual values - there is no dynamic
     *            resolution done with these.
     * @param params
     */
    private void run(Place p, MessageProducer pjProducer, Session session, UUID calendarOccurrenceID, boolean updateCalendarPointer,
            boolean outOfPlan, boolean outOfChainLaunch, UUID level0Id, UUID level1Id, UUID level2Id, UUID level3Id,
            CalendarPointer cpToUpdate, DateTime virtualTime, Map<String, String> fieldOverloads, EnvironmentValue... params)
    {
        DateTime now = DateTime.now();

        PipelineJob pj = new PipelineJob(fieldOverloads);

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
        pj.addEnvValueToCache("CHR_JOBNAME", this.getEventSourceDefinition().getName());
        pj.addEnvValueToCache("CHR_PLACEID", p.id.toString());
        pj.addEnvValueToCache("CHR_PLACENAME", p.name);
        pj.addEnvValueToCache("CHR_PLACEGROUPID", this.getRunsOn().getId().toString());
        pj.addEnvValueToCache("CHR_PLACEGROUPNAME", this.getRunsOn().getName());

        // Calendar update
        if (this.usesCalendar())
        {
            FunctionalSequence c = this.application.getCalendar(this.dto.getCalendarId());
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
        log.debug(String.format("State (%s - chain %s) was enqueued for launch on place %s", this.getEventSourceDefinition().getName(),
                this.getContainerName(), p.name));
    }

    // Create and post PJ for run
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Calendar stuff

    // Called within an open transaction. Won't be committed here.
    public void createPointers(Connection conn)
    {
        if (!this.usesCalendar())
        {
            return;
        }
        FunctionalSequence cal = this.getCalendar();

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
                // We set current = next. That way we do not loose the first occurrence in the sequence, and avoid null values (this is the
                // only case where they would be meaningful - and its an edge case, so we prefer forbidding nulls and get errors if anyone
                // plays with null values elsewhere)
                FunctionalOccurrence cd = cal.getCurrentOccurrence(conn);
                // FunctionalOccurrence cdLast = cal.getOccurrenceShiftedBy(cd, this.dto.getCalendarShift());
                FunctionalOccurrence cdNext = cal.getOccurrenceShiftedBy(cd, this.dto.getCalendarShift());
                CalendarPointer tmp = new CalendarPointer();
                tmp.setApplication(this.application);
                tmp.setCalendar(cal);
                tmp.setLastEndedOkOccurrenceCd(cdNext);
                tmp.setLastEndedOccurrenceCd(cdNext);
                tmp.setLastStartedOccurrenceCd(cdNext);
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
            log.debug(String.format("State %s (%s - chain %s) has created %s calendar pointer(s).", this.id,
                    this.getEventSourceDefinition().getName(), this.getContainerName(), i));
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
        FunctionalSequence cal = this.getCalendar();

        log.debug(String.format("State %s (%s - chain %s) uses a calendar. Calendar analysis begins.", this.id,
                this.getEventSourceDefinition().getName(), this.getContainerName()));

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
                    this.getEventSourceDefinition().getName(), this.getContainerName()));
            return false;
        }

        FunctionalOccurrence nextRunOccurrence = cal.getOccurrence(cp.getNextRunOccurrenceId());
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
        FunctionalOccurrence baseLimit = cal.getCurrentOccurrence(conn);

        log.debug(String.format("Calendar limit is currently: %s. Shift is %s, next occurrence to run for this state is %s",
                baseLimit.label, this.dto.getCalendarShift(), nextRunOccurrence.label));
        FunctionalOccurrence shiftedLimit = cal.getOccurrenceShiftedBy(baseLimit, this.dto.getCalendarShift());

        // Shift: -1 means that the State will run at D-1 when the reference is D. Therefore it should stop one occurrence before the
        // others.
        if (!cal.isBeforeOrSame(nextRunOccurrence, shiftedLimit))
        {
            log.debug(String.format(
                    "This is too soon to launch the job: calendar is at %s (with shift , this limit becomes %s), while this state wants to already run %s",
                    baseLimit.label, shiftedLimit.label, nextRunOccurrence.label));
            return false;
        }

        // If here, alles gut.
        log.debug(String.format("State %s (%s - chain %s) can run according to its calendar.", this.id,
                this.getEventSourceDefinition().getName(), this.getContainerName()));
        return true;
    }

    public boolean isLate(Connection conn, Place p)
    {
        // LATE MEANS: State Latest OK < Calendar current + State shift
        FunctionalSequence cal = this.getCalendar();
        FunctionalOccurrence cd = this.getCurrentCalendarOccurrence(conn, p);
        FunctionalOccurrence limit = cal.getOccurrenceShiftedBy(cal.getCurrentOccurrence(conn), this.dto.getCalendarShift());

        return cal.isBefore(cd, limit);
    }

    public Boolean usesCalendar()
    {
        return this.dto.getCalendarId() != null;
    }

    public FunctionalOccurrence getCurrentCalendarOccurrence(Connection conn, Place p)
    {
        return this.getCalendar().getOccurrence(this.getCurrentCalendarPointer(conn, p).getLastEndedOkOccurrenceUuid());
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
    ///////////////////////////////////////////////////////////////////////////
}
