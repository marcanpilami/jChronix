package org.chronix.chronix.source.basic.dto;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oxymores.chronix.core.source.api.EventSourceExternalyTriggered;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.source.basic.reg.ExternalBehaviour;

public class External extends EventSourceExternalyTriggered
{
    private static final long serialVersionUID = -1686612421751399022L;

    String machineRestriction, accountRestriction;
    String regularExpression;

    ///////////////////////////////////////////////////////////////////////////
    // CONSTRUCTION
    ///////////////////////////////////////////////////////////////////////////

    protected External()
    {}

    public External(String name, String description)
    {
        this(name, description, null, null, null);
    }

    public External(String name, String description, String machineRestriction, String accountRestriction, String regularExpression)
    {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.machineRestriction = machineRestriction;
        this.accountRestriction = accountRestriction;
        this.regularExpression = regularExpression;
    }

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return ExternalBehaviour.class;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Pattern
    ///////////////////////////////////////////////////////////////////////////

    public String getCalendarString(String data)
    {
        if (regularExpression == null || "".equals(regularExpression))
        {
            return null;
        }

        Pattern p = Pattern.compile(regularExpression);
        Matcher m = p.matcher(data);

        if (m.find())
        {
            return m.group(1);
        }
        else
        {
            return null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUN
    ///////////////////////////////////////////////////////////////////////////

}
