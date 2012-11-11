package org.oxymores.chronix.internalapi;

import java.util.Date;
import java.util.List;

import org.oxymores.chronix.core.timedata.RunLog;

public interface IServiceConsole
{
	List<RunLog> getLog(Date from, Date to);
}
