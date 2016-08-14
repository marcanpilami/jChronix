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
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.Range;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.NamedApplicationObject;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.engine.helpers.SenderHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * A functional sequence represents is a negative constraint. A state associated to a sequence must be launched once by occurrence of the
 * sequence and must be launched in sequence order.
 */
public class FunctionalSequence extends NamedApplicationObject
{
    private static Logger log = LoggerFactory.getLogger(FunctionalSequence.class);
    private static final long serialVersionUID = 7332812989443095188L;

    @NotNull
    @Range(min = 0, max = 300)
    protected Integer alertThreshold = 20;

    @NotNull
    protected transient ArrayList<State> usedInStates;

    @NotNull
    @Size(message = "A functional sequence must have at least one occurrence", min = 1)
    protected ArrayList<FunctionalOccurrence> occurrences;

    // Constructor
    public FunctionalSequence()
    {
        super();
        usedInStates = new ArrayList<State>();
        occurrences = new ArrayList<FunctionalOccurrence>();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stupid get/set

    public Integer getAlertThreshold()
    {
        return this.alertThreshold;
    }

    public List<State> getUsedInStates()
    {
        return this.usedInStates;
    }

    public void setAlertThreshold(Integer alertThreshold)
    {
        this.alertThreshold = alertThreshold;
    }

    public ArrayList<FunctionalOccurrence> getOccurrences()
    {
        return occurrences;
    }

    public FunctionalOccurrence getOccurrence(UUID id)
    {
        for (FunctionalOccurrence cd : this.occurrences)
        {
            if (cd.getId().equals(id))
            {
                return cd;
            }
        }
        return null;
    }

    public FunctionalOccurrence getOccurrence(String label)
    {
        for (FunctionalOccurrence cd : this.occurrences)
        {
            if (cd.getLabel().equals(label))
            {
                return cd;
            }
        }
        return null;
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Setters on relationships

    // Only called from State.addSequence
    void s_addStateUsing(State s)
    {
        usedInStates.add(s);
    }

    // Only called from State.addSequence
    void s_removeStateUsing(State s)
    {
        try
        {
            usedInStates.remove(s);
        }
        finally
        { // do nothing if asked to remove a non existent state
        }
    }

    public void addDay(FunctionalOccurrence d)
    {
        if (!this.occurrences.contains(d))
        {
            this.occurrences.add(d);
        }
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Operational data
    public CalendarPointer getCurrentOccurrencePointer(Connection conn)
    {
        // Calendar current occurrence pointers have no states and places: they are only related to the calendar itself.
        return conn
                .createQuery("SELECT * FROM CalendarPointer p WHERE p.stateID IS NULL AND p.placeID IS NULL AND p.calendarID = :calendarID")
                .addParameter("calendarID", this.id.toString()).executeAndFetchFirst(CalendarPointer.class);
    }

    public FunctionalOccurrence getCurrentOccurrence(Connection conn)
    {
        CalendarPointer currentPointer = this.getCurrentOccurrencePointer(conn);
        return currentPointer == null ? null : this.getOccurrence(this.getCurrentOccurrencePointer(conn).getLastEndedOkOccurrenceUuid());
    }

    public FunctionalOccurrence getOccurrenceAfter(FunctionalOccurrence d)
    {
        return getOccurrenceShiftedBy(d, 1);
    }

    public FunctionalOccurrence getOccurrenceShiftedBy(FunctionalOccurrence origin, int shift)
    {
        return this.occurrences.get(this.occurrences.indexOf(origin) + shift);
    }

    public FunctionalOccurrence getFirstOccurrence()
    {
        return this.occurrences.get(0);
    }

    public Boolean isBeforeOrSame(FunctionalOccurrence before, FunctionalOccurrence after)
    {
        return this.occurrences.indexOf(before) <= this.occurrences.indexOf(after);
    }

    public Boolean isBefore(FunctionalOccurrence before, FunctionalOccurrence after)
    {
        return this.occurrences.indexOf(before) < this.occurrences.indexOf(after);
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Don't go overboard...
    public boolean warnNotEnoughOccurrencesLeft(Connection conn)
    {
        FunctionalOccurrence cd = this.getCurrentOccurrence(conn);
        int onow = this.occurrences.indexOf(cd);

        return onow + this.alertThreshold >= this.occurrences.size();
    }

    public boolean errorNotEnoughOccurrencesLeft(Connection conn)
    {
        FunctionalOccurrence cd = this.getCurrentOccurrence(conn);
        int onow = this.occurrences.indexOf(cd);

        return onow + this.alertThreshold / 2 >= this.occurrences.size();
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Stragglers
    public class StragglerIssue
    {
        public State s;
        public Place p;
    }

    public List<StragglerIssue> getStragglers(Connection conn)
    {
        List<StragglerIssue> issues = new ArrayList<>();
        StragglerIssue tmp = null;

        for (State s : this.usedInStates)
        {
            for (Place p : s.getRunsOn().getPlaces())
            {
                if (s.isLate(conn, p))
                {
                    tmp = new StragglerIssue();
                    tmp.p = p;
                    tmp.s = s;
                    issues.add(tmp);
                }
            }
        }
        return issues;
    }

    public void processStragglers(Connection conn)
    {
        log.debug(String.format("Processing stragglers on calendar %s", this.name));
        FunctionalOccurrence d = this.getCurrentOccurrence(conn);
        for (StragglerIssue i : getStragglers(conn))
        {
            log.warn(String.format(
                    "State %s on place %s (in chain %s) is now late according to its calendar: it has only finished %s while it should be ready to run %s shifted by %s",
                    i.s.getEventSourceDefinition().getName(), i.p.name, "chain name", i.s.getCurrentCalendarOccurrence(conn, i.p).label,
                    d.label, i.s.getCalendarShift()));
        }
    }

    //
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Creation and update of advancement pointers

    /**
     * A minimal set of pointers must be created for a sequence to become operational. This is called at startup. <br>
     * Called within an open transaction. Won't be committed here.
     */
    public void initCalendar(Connection conn)
    {
        // Index the states using the calendar
        this.usedInStates = new ArrayList<>();
        for (State s : this.application.getStates())
        {
            if (s.getCalendar() != null && s.getCalendar().equals(this))
            {
                this.usedInStates.add(s);
            }
        }

        // Create if needed the reference calendar pointer
        if (getCurrentOccurrence(conn) != null)
        {
            return;
        }

        int minShift = Integer.MAX_VALUE;
        for (State s : usedInStates)
        {
            if (s.getCalendarShift() < minShift)
            {
                minShift = s.getCalendarShift();
            }
        }
        //minShift--;

        FunctionalOccurrence cd = this.getOccurrenceShiftedBy(this.getFirstOccurrence(), -minShift);
        log.info(String.format(
                "Calendar %s current value will be initialised at its first allowed occurrence by the shifts of the client states (max shift is %s): %s",
                this.name, minShift, cd.getLabel()));

        CalendarPointer tmp = new CalendarPointer();
        tmp.setApplication(this.application);
        tmp.setCalendar(this);
        tmp.setLastEndedOkOccurrenceCd(cd);
        tmp.setLastEndedOccurrenceCd(cd);
        tmp.setLastStartedOccurrenceCd(cd);
        tmp.setPlace(null);
        tmp.setState(null);

        tmp.insertOrUpdate(conn);

        // Init if needed the pointers of our client states
        for (State s : this.usedInStates)
        {
            s.createPointers(conn);
        }
    }

    /**
     * Advances the main sequence reference pointer by one. This does not touch states pointers, only the sequence reference pointer.<br>
     * It is intended to be called within an active JMS transaction (this method does not commit it).
     * 
     * @param envt
     * @param jmsSession
     * @param conn
     * @param jmsProducer
     */
    public void advanceByOne(Environment envt, Session jmsSession, Connection conn, MessageProducer jmsProducer)
    {
        log.debug(String.format("Calendar %s current occurrence will now be updated", this.getName()));

        CalendarPointer cp = this.getCurrentOccurrencePointer(conn);
        FunctionalOccurrence oldCd = this.getCurrentOccurrence(conn);
        FunctionalOccurrence newCd = this.getOccurrenceAfter(oldCd);
        FunctionalOccurrence nextCd = this.getOccurrenceAfter(newCd);

        log.info(String.format("Calendar %s will go from %s to %s", this.getName(), oldCd.getLabel(), newCd.getLabel()));

        if (this.warnNotEnoughOccurrencesLeft(conn) && !this.errorNotEnoughOccurrencesLeft(conn))
        {
            log.warn(String.format("Sequence %s will soon reach its end: add more occurrences to it", this.getName()));
        }
        else if (this.errorNotEnoughOccurrencesLeft(conn))
        {
            log.error(String.format("Sequence %s is nearly at its end: add more occurrences to it", this.getName()));
        }

        cp.setLastEndedOccurrenceCd(newCd);
        cp.setLastEndedOkOccurrenceCd(newCd);
        cp.setLastStartedOccurrenceCd(newCd);
        cp.setNextRunOccurrenceCd(nextCd);

        try
        {
            SenderHelpers.sendCalendarPointer(cp, this, jmsSession, jmsProducer, false, envt);
        }
        catch (JMSException e)
        {
            log.error("Could not advance calendar to its next occurrence. It will need to be manually changed", e);
        }
    }

    //
    ///////////////////////////////////////////////////////////////////////////

}
