package org.oxymores.chronix.core.source.api;

import java.util.UUID;

import org.joda.time.DateTime;

/**
 * A JobDescription gives all which is needed to run a job: the source that should run, the different ID...
 *
 */
public interface JobDescription
{
    /**
     * The ID of the {@link EventSource} describing the event source that should run.
     */
    public UUID getEventSourceId();

    /**
     * The ID associated to the launch described by this object.
     */
    public UUID getLaunchId();

    /**
     * The ID associated to the launch of the current scope. (e.g.: if the scope is defined by a chain, this is the launch ID of that chain)
     */
    public UUID getParentScopeLaunchId();

    /**
     * The scheduler does not use "real time" (as in "system time") internally, but a "virtual time" that can be fully disconnected from
     * "real time". A good example of disconnection is when the engine is doing a simulation: in a moment, virtual time can leap hours to
     * simulate a full day.<br>
     * This gives access to the virtual time corresponding to the start of execution.
     * 
     * @return
     */
    public DateTime getVirtualTimeStart();

    /**
     * Some launches are done without event throwing and calendar updating. Sources may want to know it.
     */
    public boolean isOutOfPlan();

}
