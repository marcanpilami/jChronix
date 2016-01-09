package org.oxymores.chronix.core.engine.api;

import java.io.File;
import java.util.UUID;

import org.oxymores.chronix.dto.HistoryQuery;

/**
 * This service is the only public way of interacting with the execution history. It offers method to filter and sort the jobs and retrieve
 * the log files.
 */
public interface HistoryService
{
    /**
     * Runs a history query. Note that the returned result is the same object as the on given in argument.
     */
    public HistoryQuery query(HistoryQuery q);

    /**
     * Returns the short log (if any, otherwise an empty string) of the specified launch.
     */
    public String getShortLog(UUID id);

    /**
     * Returns a pointer on the file containing the full job log.
     */
    public File getLogFile(UUID launchId);
}
