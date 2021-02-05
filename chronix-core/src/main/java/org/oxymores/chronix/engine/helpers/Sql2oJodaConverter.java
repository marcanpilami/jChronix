package org.oxymores.chronix.engine.helpers;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.sql2o.converters.Converter;
import org.sql2o.converters.ConverterException;

public class Sql2oJodaConverter implements Converter<DateTime>
{
    private final DateTimeZone timeZone;

    public Sql2oJodaConverter(DateTimeZone timeZone)
    {
        this.timeZone = timeZone;
    }

    public Sql2oJodaConverter()
    {
        this(DateTimeZone.getDefault());
    }

    public DateTime convert(Object val) throws ConverterException
    {
        if (val == null)
        {
            return null;
        }
        try
        {
            if (val instanceof OffsetDateTime)
            {
                val = ((OffsetDateTime)val).toEpochSecond();
            }
            // Joda has it's own pluggable converters infrastructure
            // it will throw IllegalArgumentException if can't convert
            // look @ org.joda.time.convert.ConverterManager
            return new LocalDateTime(val).toDateTime(timeZone);
        }
        catch (IllegalArgumentException ex)
        {
            throw new ConverterException("Error while converting type " + val.getClass().toString() + " to jodatime", ex);
        }
    }

    public Object toDatabaseParam(DateTime val)
    {
        return new Timestamp(val.getMillis());
    }
}
