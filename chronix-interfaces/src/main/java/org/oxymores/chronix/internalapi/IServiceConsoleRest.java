package org.oxymores.chronix.internalapi;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.oxymores.chronix.dto.DTORunLog;
import org.oxymores.chronix.dto.ResOrder;

public interface IServiceConsoleRest
{
    public List<DTORunLog> getLog(Date from, Date to, Date since);

    public List<DTORunLog> getLogSince(Date since);

    public ArrayList<DTORunLog> getLog();

    public String getShortLog(UUID id);

    public ResOrder orderForceOK(String launchId);

    public File getLogFile(String launchId);
}
