package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;

public class EventSourceRunResult implements Serializable
{
    private static final long serialVersionUID = -8309267933217251096L;

    /**
     * A maximum of 255 characters giving a summary (or the beginning) of the log file. Null if no log file.
     */
    public String logStart = null;

    /**
     * A maximum of 4000 characters containing the full log file, or 1000 character from the beginning + 3000 from the end if too long.<br>
     * null if no log file. If null & logStart isn't, logStart is copied here.
     */
    public String fullerLog = null;

    /**
     * The path to the complete log file (where the log was created). Relative to the base log directory.<br>
     * null if no log file.<br>
     * If this is specified, logSizeBytes must be strictly greater than zero.
     */
    public String logPath = null;

    /**
     * The exact size of logPath in bytes.
     */
    public Long logSizeBytes = null;

    /**
     * A return code. The interpretation of this code is left to the {@link EventSourceProvider}, but a recommendation is to use 0 for a
     * success. It must be set for the {@link EventSourceRunResult} to be valid.
     */
    public Integer returnCode = null;

    /**
     * A high level interpretation of the result of the run: can this result be considered as a success or not? This is important most
     * notably for logs (red or not!) and for calendar advancement (only touch the calendar if OK)<br>
     * If left to <code>null</code>, the engine will test {@link #returnCode} == 0 instead. Default value is <code>null</code>.
     */
    public Boolean success = null;

    /**
     * These items will be added to the environment and made available to downstream states. Default is none.
     */
    public Map<String, String> newEnvVars = new HashMap<>();

    /**
     * The time when the job has actually ended. Default is system clock.<br>
     * Default should be fine for most sources (except those which have asynchronous sub executions)<br>
     * Note that the start time is set by the engine itself and is not available in this object.<br>
     */
    public DateTime end = DateTime.now();

    /**
     * An {@link EventSourceRunResult} is a result for a given launch, designated by its launch ID. By default, the launch is of course the
     * current one, and this field is not used. But in some cases a source may want to return a result for another launch. In these cases,
     * the launch ID of the target launch must be given in this field. <br>
     * Default (which should be OK for most plugins) is to use null. This means the result is for the current launch.
     */
    public UUID overloadedLaunchId = null;

    /**
     * Used to overload the scope in which the events created by the result of the run will be created.<br>
     * Default (which should be OK for most plugins) is to use null. This means the scope is not changed.
     */
    public UUID overloadedScopeId = null;

    /**
     * Used to overload the calendar/sequence corresponding to this launch. Events created will correspond to this occurrence.<br>
     * Note it only changes the occurrence in the events - it has no impact on the sequence advancement for the currently run event source.
     */
    public String overloadedSequenceOccurrence = null;
}
