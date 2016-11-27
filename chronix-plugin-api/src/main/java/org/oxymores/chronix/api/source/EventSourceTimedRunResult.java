package org.oxymores.chronix.api.source;

import org.joda.time.DateTime;

public class EventSourceTimedRunResult extends EventSourceRunResult
{
    private static final long serialVersionUID = -2961807937067388106L;

    /**
     * The virtual time at which the source needs to be run once again. The {@link RunModeTimeTriggered#run(DTOEventSource, JobDescription)}
     * method will be called with exactly this virtual time inside the {@link JobDescription#getVirtualTimeStart()} argument<br>
     * Not setting this will cause an {@link IllegalArgumentException}.
     */
    public DateTime callMeBackAt = null;
}
