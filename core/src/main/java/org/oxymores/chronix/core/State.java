/**
 * @author Marc-Antoine Gouillart
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
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.oxymores.chronix.core.transactional.CalendarPointer;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.oxymores.chronix.core.transactional.PipelineJob;
import org.oxymores.chronix.engine.SenderHelpers;

public class State extends ConfigurableBase {
	private static Logger log = Logger.getLogger(State.class);

	private static final long serialVersionUID = -2640644872229489081L;

	protected Boolean parallel = false;

	// GUI data
	protected Integer x, y;

	// Time limits
	protected Integer warnAfterMn, killAfterMn, maxPipeWaitTime,
			eventValidityMn;

	// The active element represented by this State
	protected ActiveNodeBase represents;

	// The chain it belongs to
	protected Chain chain;

	// Transitions
	protected ArrayList<Transition> trFromHere, trReceivedHere;

	// Exclusive states
	protected ArrayList<State> exclusiveStates;

	// Runs on a group. Groups are defined in a separate graph, so get it by ID
	// and not by reference.
	protected PlaceGroup runsOn;

	// Sequences
	protected ArrayList<AutoSequence> sequences;
	protected Calendar calendar;
	protected Boolean loopMissedOccurrences;
	protected Boolean endOfOccurrence;
	protected Boolean blockIfPreviousFailed = false;
	protected int calendarShift = 0;

	public Boolean getLoopMissedOccurrences() {
		return loopMissedOccurrences;
	}

	public void setLoopMissedOccurrences(Boolean loopMissedOccurrences) {
		this.loopMissedOccurrences = loopMissedOccurrences;
	}

	public Boolean getEndOfOccurrence() {
		return endOfOccurrence;
	}

	public void setEndOfOccurrence(Boolean endOfOccurrence) {
		this.endOfOccurrence = endOfOccurrence;
	}

	public State() {
		super();
		this.exclusiveStates = new ArrayList<State>();
		this.trFromHere = new ArrayList<Transition>();
		this.trReceivedHere = new ArrayList<Transition>();
		this.sequences = new ArrayList<AutoSequence>();
	}

	public PlaceGroup getRunsOn() {
		return this.runsOn;
	}

	public ArrayList<Place> getRunsOnPlaces() {
		return this.runsOn.getPlaces();
	}

	public ArrayList<ExecutionNode> getRunsOnExecutionNodes() {
		ArrayList<ExecutionNode> res = new ArrayList<ExecutionNode>();
		for (Place p : this.runsOn.getPlaces()) {
			if (!res.contains(p.getNode())) {
				res.add(p.getNode());
			}
		}
		return res;
	}

	public ArrayList<ExecutionNode> getRunsOnPhysicalNodes() {
		ArrayList<ExecutionNode> all = getRunsOnExecutionNodes();
		ArrayList<ExecutionNode> res = getRunsOnExecutionNodes();
		for (ExecutionNode n : all) {
			if (n.isHosted()) {
				if (!res.contains(n.getHost()))
					res.add(n.getHost());
			} else {
				// Not hosted - true Physical Node
				if (!res.contains(n))
					res.add(n);
			}
		}
		return res;
	}

	public void setRunsOn(PlaceGroup group) {
		this.runsOn = group;
	}

	public void addTransitionFromHere(Transition tr) {
		if (!this.trFromHere.contains(tr)) {
			this.trFromHere.add(tr);
			tr.setStateFrom(this);
		}
	}

	public void addTransitionReceivedHere(Transition tr) {
		if (!this.trReceivedHere.contains(tr)) {
			this.trReceivedHere.add(tr);
			tr.setStateTo(this);
		}
	}

	public Transition connectTo(State target) {
		return connectTo(target, 0, null, null, null);
	}

	public Transition connectTo(State target, Integer guard1) {
		return connectTo(target, guard1, null, null, null);
	}

	public Transition connectTo(State target, Integer guard1, String guard2,
			String guard3, UUID guard4) {
		// Note: there can be multiple transitions between two states.
		Transition t = new Transition();
		t.setStateFrom(this);
		t.setStateTo(target);
		t.setGuard1(guard1);
		t.setGuard2(guard2);
		t.setGuard3(guard3);
		t.setGuard4(guard4);
		t.setApplication(this.application);
		this.chain.addTransition(t);
		return t;
	}

	public ArrayList<Transition> getTrFromHere() {
		return trFromHere;
	}

	public ArrayList<State> getClientStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition t : trFromHere) {
			res.add(t.stateTo);
		}
		return res;
	}

	public ArrayList<State> getParentStates() {
		ArrayList<State> res = new ArrayList<State>();
		for (Transition t : trReceivedHere) {
			res.add(t.stateFrom);
		}
		return res;
	}

	public ArrayList<Transition> getTrReceivedHere() {
		return trReceivedHere;
	}

	public ArrayList<AutoSequence> getSequences() {
		return sequences;
	}

	public Calendar getCalendar() {
		return calendar;
	}

	public Boolean getParallel() {
		return parallel;
	}

	public void setParallel(Boolean parallel) {
		this.parallel = parallel;
	}

	public Integer getX() {
		return x;
	}

	public void setX(Integer x) {
		this.x = x;
	}

	public Integer getY() {
		return y;
	}

	public void setY(Integer y) {
		this.y = y;
	}

	public Integer getWarnAfterMn() {
		return warnAfterMn;
	}

	public void setWarnAfterMn(Integer warnAfterMn) {
		this.warnAfterMn = warnAfterMn;
	}

	public Integer getKillAfterMn() {
		return killAfterMn;
	}

	public void setKillAfterMn(Integer killAfterMn) {
		this.killAfterMn = killAfterMn;
	}

	public Integer getMaxPipeWaitTime() {
		return maxPipeWaitTime;
	}

	public void setMaxPipeWaitTime(Integer maxPipeWaitTime) {
		this.maxPipeWaitTime = maxPipeWaitTime;
	}

	public Integer getEventValidityMn() {
		return eventValidityMn;
	}

	public void setEventValidityMn(Integer eventValidityMn) {
		this.eventValidityMn = eventValidityMn;
	}

	public ActiveNodeBase getRepresents() {
		return represents;
	}

	public void setRepresents(ActiveNodeBase represents) {
		this.represents = represents;
	}

	public Chain getChain() {
		return chain;
	}

	public void setChain(Chain chain) {
		this.chain = chain;
		this.application = chain.getApplication();
		if (chain != null)
			chain.addState(this);
	}

	public ArrayList<State> getExclusiveStates() {
		return exclusiveStates;
	}

	public void addSequence(AutoSequence s) {
		s.s_addStateUsing(this);
		this.sequences.add(s);
	}

	public void removeSequence(AutoSequence s) {
		try {
			this.sequences.remove(s);
		} finally {
			s.s_removeStateUsing(this);
		}
	}

	public void setCalendar(Calendar c) {
		c.s_addStateUsing(this);
		this.calendar = c;
	}

	public void removeCalendar() {
		if (this.calendar != null) {
			this.calendar.s_removeStateUsing(this);
			this.calendar = null;
		}
	}

	public Boolean usesCalendar() {
		return this.calendar != null;
	}

	public CalendarDay getCurrentCalendarOccurrence(EntityManager em, Place p)
			throws Exception {
		return this.calendar.getDay(this.getCurrentCalendarPointer(em, p)
				.getLastEndedOkOccurrenceUuid());
	}

	public CalendarPointer getCurrentCalendarPointer(EntityManager em, Place p)
			throws Exception {
		if (!usesCalendar())
			throw new Exception(
					"A state without calendar has no current occurrence");

		Query q = em
				.createQuery("SELECT p FROM CalendarPointer p WHERE p.stateID = ?1 AND p.placeID = ?2 AND p.calendarID = ?3");
		q.setParameter(1, this.id.toString());
		q.setParameter(2, p.id.toString());
		q.setParameter(3, this.calendar.id.toString());
		CalendarPointer cp = (CalendarPointer) q.getSingleResult();
		em.refresh(cp);
		return cp;

	}

	public void consumeEvents(List<Event> events, List<Place> places,
			EntityManager em) {
		for (Event e : events) {
			for (Place p : places) {
				EventConsumption ec = new EventConsumption();
				ec.setEvent(e);
				ec.setPlace(p);
				ec.setState(this);

				em.persist(ec);

				log.debug(String.format(
						"Event %s marked as consumed on place %s", e.getId(),
						p.name));
			}
		}
	}

	public void runAlone(Place p, MessageProducer pjProducer, Session session) {
		run(p, pjProducer, session, null, false, true, true, this.chain.id,
				UUID.randomUUID(), null);
	}

	public void runInsideChainWithoutUpdatingCalendar(Place p,
			MessageProducer pjProducer, Session session) {
		run(p, pjProducer, session, null, false, false, false, this.chain.id,
				UUID.randomUUID(), null);
	}

	public void runFromEngine(Place p, EntityManager em,
			MessageProducer pjProducer, Session session, Event e) {
		// Calendar update
		String CalendarOccurrenceID = null;
		if (this.usesCalendar()) {
			try {
				CalendarPointer cp = this.getCurrentCalendarPointer(em, p);
				CalendarOccurrenceID = cp.getNextRunOccurrenceId();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		CalendarPointer cpToUpdate = null;
		if (this.usesCalendar()) {
			try {
				cpToUpdate = this.getCurrentCalendarPointer(em, p);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		run(p, pjProducer, session, CalendarOccurrenceID, true, false,
				e.getOutsideChain(), e.getLevel0IdU(), e.getLevel1IdU(),
				cpToUpdate);
	}

	public void run(Place p, MessageProducer pjProducer, Session session,
			String CalendarOccurrenceID, boolean updateCalendarPointer,
			boolean outOfPlan, boolean outOfChainLaunch, UUID level0Id,
			UUID level1Id, CalendarPointer cpToUpdate) {
		DateTime now = DateTime.now();

		PipelineJob pj = new PipelineJob();
		pj.setLevel0IdU(level0Id);
		pj.setLevel1IdU(level1Id);
		pj.setMarkedForRunAt(now.toDate());
		pj.setPlace(p);
		pj.setState(this);
		pj.setStatus("ENTERING_QUEUE");
		pj.setApplication(this.application);
		pj.setOutsideChain(outOfChainLaunch);
		pj.setIgnoreCalendarUpdating(!updateCalendarPointer);
		pj.setOutOfPlan(outOfPlan);

		if (this.warnAfterMn != null)
			pj.setWarnNotEndedAt(now.plusMinutes(this.warnAfterMn).toDate());
		else
			pj.setWarnNotEndedAt(now.plusDays(1).toDate());

		if (this.killAfterMn != null)
			pj.setKillAt(now.plusMinutes(this.killAfterMn).toDate());

		// Calendar update
		if (this.usesCalendar()) {
			try {
				pj.setCalendarOccurrenceID(CalendarOccurrenceID);
				pj.setCalendar(calendar);

				log.debug("Since this state will run, calendar update!");
				cpToUpdate.setRunning(true);
				if (updateCalendarPointer)
					cpToUpdate.setLastStartedOccurrenceId(cpToUpdate
							.getNextRunOccurrenceId());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

		// Send it (commit is done by main engine later)
		String qName = String.format("Q.%s.PJ", p.getNode().getBrokerName());
		try {
			SenderHelpers.sendPipelineJobToRunner(pj, p.getNode().getHost(),
					pjProducer, session, false);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Done
		log.debug(String
				.format("State (%s - chain %s) was enqueued for launch on place %s (queue %s)",
						this.represents.getName(), this.chain.getName(),
						p.name, qName));
	}

	// Called within an open transaction. Won't be committed here.
	public void createPointers(EntityManager em) {
		if (!this.usesCalendar())
			return;

		// Get existing pointers
		TypedQuery<CalendarPointer> q = em.createQuery(
				"SELECT p FROM CalendarPointer p WHERE p.stateID = ?1",
				CalendarPointer.class);
		q.setParameter(1, this.id.toString());
		List<CalendarPointer> ptrs = q.getResultList();

		// A pointer should exist on all places
		int i = 0;
		for (Place p : this.runsOn.places) {
			// Is there a existing pointer on this place?
			CalendarPointer existing = null;
			for (CalendarPointer retrieved : ptrs) {
				if (retrieved.getPlaceID().equals(p.id.toString())) {
					existing = retrieved;
					break;
				}
			}

			// If not, create one
			if (existing == null) {
				// A pointer should be created on this place!
				CalendarPointer tmp = new CalendarPointer();
				tmp.setApplication(this.application);
				tmp.setCalendar(this.calendar);
				tmp.setLastEndedOkOccurrenceCd(this.calendar
						.getFirstOccurrence());
				tmp.setLastEndedOccurrenceCd(this.calendar.getFirstOccurrence());
				tmp.setLastStartedOccurrenceCd(this.calendar
						.getFirstOccurrence());
				tmp.setNextRunOccurrenceCd(this.calendar.getFirstOccurrence());
				tmp.setPlace(p);
				tmp.setState(this);
				i++;

				em.persist(tmp);
			}
		}

		if (i != 0)
			log.debug(String
					.format("State %s (%s - chain %s) has created %s calendar pointer(s).",
							this.id, this.represents.name, this.chain.name, i));

		// Commit is done by the calling method
	}

	public boolean canRunAccordingToCalendarOnPlace(EntityManager em, Place p) {
		if (!this.usesCalendar()) {
			log.debug("Does not use a calendar - crontab mode");
			return true; // no calendar = no calendar constraints
		}

		log.debug(String
				.format("State %s (%s - chain %s) uses a calendar. Calendar analysis begins.",
						this.id, this.represents.name, this.chain.name));

		// Get the pointer
		Query q = em
				.createQuery("SELECT e FROM CalendarPointer p WHERE p.stateID = ?1 AND p.placeID = ?2");
		q.setParameter(1, this.id.toString());
		q.setParameter(2, p.getId().toString());

		CalendarPointer cp = null;
		try {
			cp = this.getCurrentCalendarPointer(em, p);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (cp == null) {
			log.error(String
					.format("State %s (%s - chain %s): CalendarPointer is null - should not be possible. It's a bug.",
							this.id, this.represents.name, this.chain.name));
			return false;
		}

		// CalendarDay lastStartedOccurrence = this.calendar.getDay(UUID
		// .fromString(cp.getLastStartedOccurrenceId()));
		// CalendarDay lastEndedOccurrence = this.calendar.getDay(UUID
		// .fromString(cp.getLastEndedOccurrenceId()));
		// CalendarDay lastEndedOKOccurrence = this.calendar.getDay(UUID
		// .fromString(cp.getLastEndedOkOccurrenceId()));
		CalendarDay nextRunOccurrence = this.calendar.getDay(UUID.fromString(cp
				.getNextRunOccurrenceId()));

		// Only one occurrence can run at the same time
		if (cp.getRunning()) {
			log.debug("Previous run has not ended - it must end for a new run to occur");
			return false;
		}

		// Only run if previous run was OK (unless asked for)
		if (cp.getLatestFailed() && this.blockIfPreviousFailed) {
			log.debug("Previous run has ended incorrectly - it must end correctly for a new run to occur");
			return false;
		}

		// Sequence must be respected
		// But actually, nothing has to be done to enforce it
		// as it comes from either the scheduler itself or the user.

		// No further than the calendar itself
		CalendarDay baseLimit = this.calendar.getCurrentOccurrence(em);
		log.debug(String
				.format("Calendar limit is currently: %s. Shift is %s, next occurrence to run for this state is %s",
						baseLimit.seq, this.calendarShift,
						nextRunOccurrence.seq));
		CalendarDay shiftedLimit = this.calendar.getOccurrenceShiftedBy(
				baseLimit, this.calendarShift);

		// Shift: -1 means that the State will run at D-1 when the reference is
		// D. Therefore it should stop one occurrence before the others.
		if (!this.calendar.isBeforeOrSame(nextRunOccurrence, shiftedLimit)) {
			log.debug(String
					.format("This is too soon to launch the job: calendar is at %s (with shift , this limit becomes %s), while this state wants to already run %s",
							baseLimit.seq, shiftedLimit.seq,
							nextRunOccurrence.seq));
			return false;
		}

		// If here, alles gut.
		log.debug(String.format(
				"State %s (%s - chain %s) can run according to its calendar.",
				this.id, this.represents.name, this.chain.name));
		return true;
	}

	public boolean isLate(EntityManager em, Place p) {
		// The state is catching the calendar, which should always be one step
		// ahead of the state.
		CalendarDay cd = null;
		try {
			cd = this.getCurrentCalendarOccurrence(em, p);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cd = this.calendar.getOccurrenceShiftedBy(cd, this.calendarShift + 1);

		return !this.calendar.isBeforeOrSame(
				this.calendar.getCurrentOccurrence(em), cd);
	}
}
