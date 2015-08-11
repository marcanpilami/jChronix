package org.oxymores.chronix.core.timedata;

import java.util.UUID;
import javax.validation.constraints.Size;
import org.joda.time.DateTime;

import org.oxymores.chronix.engine.helpers.OrderType;

/**
 Future use.
 */
public class UserTrace
{
    private static final int UUID_LENGTH = 36;
    private static final int DESCR_LENGTH = 100;

    private UUID id;

    private OrderType type;

    @Size(min = 1, max = DESCR_LENGTH)
    private String username;

    @Size(min = 1, max = DESCR_LENGTH)
    private String details;

    private DateTime submitted, ended;

    public UserTrace()
    {
        this.id = UUID.randomUUID();
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public OrderType getType()
    {
        return type;
    }

    public void setType(OrderType type)
    {
        this.type = type;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getDetails()
    {
        return details;
    }

    public void setDetails(String details)
    {
        this.details = details;
    }

    public DateTime getSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(DateTime submitted)
    {
        this.submitted = submitted;
    }

    public DateTime getEnded()
    {
        return ended;
    }

    public void setEnded(DateTime ended)
    {
        this.ended = ended;
    }
}
