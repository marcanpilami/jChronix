package org.oxymores.chronix.wapi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
public class DateHandler implements ParamConverterProvider
{
    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations)
    {
        if (rawType.isAssignableFrom(java.util.Date.class))
        {
            return (ParamConverter<T>) new ParamConverter<java.util.Date>()
            {
                @Override
                public Date fromString(String s)
                {
                    try
                    {
                        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
                        Date date = df.parse(s);
                        return date;
                    }
                    catch (ParseException e)
                    {
                        throw new WebApplicationException(new Exception("Date format should be yyyy-MM-dd'T'HH:mm:ss.S'Z'"),
                                Status.BAD_REQUEST);
                    }
                }

                @Override
                public String toString(Date value)
                {
                    // TODO Auto-generated method stub
                    return null;
                }

            };
        }

        // Otherwise, no converter.
        return null;
    }
}
