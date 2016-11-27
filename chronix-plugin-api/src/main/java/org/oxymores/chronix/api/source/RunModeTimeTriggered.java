package org.oxymores.chronix.api.source;

/**
 * An event source which is triggered by time events. The prime example of this is of course a clock: run every hour, every day except on
 * Sundays, etc.<br>
 * As is the case for all "RunModeX" event sources, the actual trigger happens inside the engine - there is no need for the source to
 * maintain a thread and trigger itself a the right time. This allows the event source plugin to concentrate on actually creating events,
 * instead of maintaining the configuration (it may change!) of all its related States that should be run, as well as avoiding to create raw
 * events. <br>
 * On each run, a source returns the "time" at which it wants to be called again. "Time" is actually a virtual time - it may be different
 * than the system clock. This allows simulations (during simulations, virtual time is completely uncorrelated to system clock) and catching
 * up (during which virtual time is in the past). So {@link RunModeTimeTriggered} providers must be very careful never to use system time,
 * but virtual time instead.<br>
 * <br>
 * It is very important to note that events created by such a source are always on the global scope - therefore, they can only be used in
 * top level containers. (It would be impossible to reliably link a time event to a specific scope, as scopes are unique per container
 * launch...)
 */
public interface RunModeTimeTriggered
{
    /**
     * The main method of a time triggered event source plugin. <br>
     * <br>
     * This type of event source creates a single event on each run. The event itself is created by the caller with the content of the
     * returned {@link EventSourceTimedRunResult} object. It is expected to be very simple and simply create events as requested.<br>
     * <strong>Beware</strong>: this method may be called at engine startup just to get the next run time. In that case, a returnCode of -2
     * is expected. No events will be created with that return code. <br>
     * This method is expected to always run <strong>synchronously</strong> and take at most a few seconds on most platforms<br>
     */
    public abstract EventSourceTimedRunResult run(DTOEventSource source, JobDescription jd);
}
