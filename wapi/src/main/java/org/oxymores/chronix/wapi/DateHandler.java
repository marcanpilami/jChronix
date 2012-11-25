package org.oxymores.chronix.wapi;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.jaxrs.ext.ParameterHandler;

public class DateHandler implements ParameterHandler<java.util.Date>
{
	@Override
	public Date fromString(String s)
	{
		try
		{
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
			Date date = df.parse(s);
			return date;
		} catch (ParseException e)
		{
			throw new WebApplicationException(new Exception("Date format should be yyyy-MM-dd'T'HH:mm:ss.S'Z'"), Status.BAD_REQUEST);
		}
	}

}
