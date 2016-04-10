package org.oxymores.chronix.api.source;

import java.util.List;
import java.util.Map;
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

    /**
     * Every event source is associated to an optional (sorted) set of parameters. What to do with them is left to the event source. For
     * example, the shell command event source will use them as arguments to the shell command line. These can use interpolation (use of
     * other variables or parameters) or be the result of another job - they are presented here fully resolved.<br>
     * The order of the list may be important to respect depending on the plugin.<br>
     * Note that this is an <code>Entry</code> list and not a Map: keys can be empty or non-unique.
     */
    public List<Map.Entry<String, String>> getParameters();

    /**
     * The resolved values corresponding to the fields of event source, as described by {@link EventSourceProvider#getFields()} <br>
     * The values have already been validated.<br>
     * This is a <code>Map</code> as field keys are unique and order is not important (fields should always be accessed by key).
     */
    public Map<String, String> getFields();
}
