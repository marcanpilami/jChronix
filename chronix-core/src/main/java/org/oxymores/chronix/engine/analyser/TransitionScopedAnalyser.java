package org.oxymores.chronix.engine.analyser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.oxymores.chronix.api.source.DTOTransition;
import org.oxymores.chronix.core.app.FunctionalOccurrence;
import org.oxymores.chronix.core.app.State;
import org.oxymores.chronix.core.network.Place;
import org.oxymores.chronix.core.transactional.Event;
import org.oxymores.chronix.core.transactional.EventConsumption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;

/**
 * Analysis of a transition scoped on a single place (a TARGET place). This represents the multiplexing of a transition.
 */
class TransitionScopedAnalyser
{
    private static final Logger log = LoggerFactory.getLogger(TransitionScopedAnalyser.class);

    /**
     * True if transition possible on specified target place.
     */
    boolean allowed = false;

    /**
     * The events that have been consumed (only if transition is possible, empty otherwise)
     */
    Set<Event> consumedEvents = new HashSet<Event>();

    /**
     * Helper field: the place on which this object is doing its analysis.
     */
    Place targetPlace;

    /**
     * Helper field: the state for which this object is doing its analysis.
     */
    State targetState;

    DTOTransition tr;

    TransitionScopedAnalyser(DTOTransition tr, State targetState, State sourceState, Place targetPlace, Collection<Event> fullScopeEvents,
            Collection<EventConsumption> allConsumptions, boolean isParallel, Connection conn)
    {
        // Note that we require consumptions in argument and select events in memory in order to do a single query for all consumptions even
        // if many analysis subdivisions. This is an optimisation - engine performance is crucial.
        this.tr = tr;
        this.targetPlace = targetPlace;
        this.targetState = targetState;
        log.debug(String.format("Analysis for transition %s on target Place %s with %s events", tr.getId(), targetPlace.getName(),
                fullScopeEvents.size()));

        // Select the events we need: only those about the source state, only those not consumed on target place.
        Set<Event> selected = new HashSet<>();
        outer: for (Event e : fullScopeEvents)
        {
            if (!e.getStateID().equals(sourceState.getId()))
            {
                continue;
            }
            for (EventConsumption ec : allConsumptions)
            {
                if (ec.getEventID().equals(e.getId()) && ec.getPlaceID().equals(targetPlace.getId()))
                {
                    continue outer;
                }
            }
            selected.add(e);
        }

        // Now do an analysis according to the source event source plugin.
        if (isParallel)
        {
            singleSourcePlaceAnalysis(tr, selected, targetPlace, conn, targetState, sourceState);
        }
        else
        {
            // In this case, transition must be OK on all source places at the same time.
            for (Place sourcePlace : sourceState.getRunsOnPlaces())
            {
                if (!singleSourcePlaceAnalysis(tr, selected, sourcePlace, conn, targetState, sourceState))
                {
                    log.debug(String.format("Analysis for transition %s on target place %s is KO", tr.getId(), sourcePlace.getName()));
                    return;
                }
            }
        }

    }

    private boolean singleSourcePlaceAnalysis(DTOTransition tr, Set<Event> events, Place sourcePlace, Connection conn, State targetState,
            State sourceState)
    {
        for (Event e : events)
        {
            if (!e.getPlaceID().equals(sourcePlace.id))
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
            if (!sourceState.getEventSourceDefinition().isTransitionPossible(tr, e))
            {
                continue;
            }

            // Check calendar if the transition is calendar-aware
            if (tr.isCalendarAware())
            {
                log.debug("Checking wether an event respects a calendar transition guard");
                if (!targetState.usesCalendar())
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
                        if (!e.getCalendarOccurrenceID()
                                .equals(targetState.getCurrentCalendarPointer(conn, sourcePlace).getNextRunOccurrenceId()))
                        {
                            FunctionalOccurrence cd1 = targetState.getCalendar().getOccurrence(e.getCalendarOccurrenceID());
                            FunctionalOccurrence cd2 = targetState.getCalendar()
                                    .getOccurrence(targetState.getCurrentCalendarPointer(conn, sourcePlace).getNextRunOccurrenceId());
                            log.debug(String.format("Rejected an event for date mismatch: got %s (in event) expected %s (in target state)",
                                    cd1.getLabel(), cd2.getLabel()));
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

            // If here: the event is OK for the given transition on the given place.
            this.consumedEvents.add(e);
            this.allowed = true;
            log.debug(String.format("Origin analysis for transition %s (from place %s toward target place %s) is OK", tr.getId(),
                    sourcePlace.getName(), targetPlace.getName()));
            return true; // no need to continue as analysis already has OK result
        }

        // If here, analysis ends on KO.
        log.debug(String.format("Analysis for transition %s (from place %s toward target place %s) is KO", tr.getId(),
                sourcePlace.getName(), targetPlace.getName()));
        this.consumedEvents.clear(); // needed: may have been OK on other places, and therefore we may have marked events for consumption.
        this.allowed = false;
        return false;
    }

    void consumeEvents(Connection conn)
    {
        if (!this.allowed)
        {
            return;
        }

        for (Event e : consumedEvents)
        {
            log.debug(String.format("Event %s marked as consumed on place %s by state %s", e.getId(), targetPlace.getName(),
                    targetState.getEventSourceDefinition().getName()));
            EventConsumption ec = new EventConsumption();
            ec.setEventID(e.getId());
            ec.setPlaceID(targetPlace.getId());
            ec.setStateID(targetState.getId());
            ec.setAppID(e.getAppID());

            ec.insert(conn);
        }
    }

    Set<Event> getUsedEvents()
    {
        if (!this.allowed)
        {
            return new HashSet<>();
        }
        return consumedEvents;
    }
}
