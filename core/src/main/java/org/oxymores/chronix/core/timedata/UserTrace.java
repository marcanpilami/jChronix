package org.oxymores.chronix.core.timedata;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.oxymores.chronix.engine.helpers.OrderType;

@Entity
public class UserTrace
{
    private static final int UUID_LENGTH = 36;
    private static final int DESCR_LENGTH = 100;

    @Column(length = UUID_LENGTH)
    @Id
    private String id;

    private OrderType type;

    @Column(length = DESCR_LENGTH)
    private String username;

    @Column(length = DESCR_LENGTH)
    private String details;

    private Date submitted, ended;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    // ////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getId()
    {
        return id;
    }

    public void setId(String id)
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

    public Date getSubmitted()
    {
        return submitted;
    }

    public void setSubmitted(Date submitted)
    {
        this.submitted = submitted;
    }

    public Date getEnded()
    {
        return ended;
    }

    public void setEnded(Date ended)
    {
        this.ended = ended;
    }
}
