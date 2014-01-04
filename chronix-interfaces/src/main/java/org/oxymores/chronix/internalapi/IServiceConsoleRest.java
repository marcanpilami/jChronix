package org.oxymores.chronix.internalapi;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.ResOrder;

public interface IServiceConsoleRest
{
    List<DTORunLog> getLog(Date from, Date to, Date since);

    List<DTORunLog> getLogSince(Date since);

    List<DTORunLog> getLog();

    String getShortLog(UUID id);

    ResOrder orderForceOK(String launchId);

    File getLogFile(String launchId);
}
