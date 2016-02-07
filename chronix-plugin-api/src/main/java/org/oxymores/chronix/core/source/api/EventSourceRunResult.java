package org.oxymores.chronix.core.source.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joda.time.DateTime;

public class EventSourceRunResult
{
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
     * A return code. The interpretation of this code is left to the {@link EventSourceBehaviour}, but a recommendation is to use 0 for a
     * success. It must be set for the {@link EventSourceRunResult} to be valid.
     */
    public Integer returnCode = null;

    /**
     * These items will be added to the environment and made available to downstream states. Default is none.
     */
    public Map<String, String> newEnvVars = new HashMap<>();

    /**
     * The time when the job has actually ended. Default is system clock.<br>
     * Note that the start time is set by the engine itself and is not available in this object.<br>
     */
    public DateTime end = DateTime.now();

    /**
     * Used to overload the scope in which the events created by the result of the run will be created.<br>
     * Default (which should be OK for most plugins) is to use null. This means the scope is not changed.
     */
    public UUID overloadedScopeId = null;
}
