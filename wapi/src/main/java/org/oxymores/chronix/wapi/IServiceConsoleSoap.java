package org.oxymores.chronix.wapi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.oxymores.chronix.dto.DTORunLog;

public interface IServiceConsoleSoap
{
	List<DTORunLog> getLog(Date from, Date to, Date since);
	ArrayList<DTORunLog> getLog();
}
