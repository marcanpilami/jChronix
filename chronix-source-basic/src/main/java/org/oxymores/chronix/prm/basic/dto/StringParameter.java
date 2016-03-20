package org.oxymores.chronix.prm.basic.dto;

import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.api.prm.ParameterProvider;
import org.oxymores.chronix.prm.basic.prv.StringParameterProvider;

/**
 * A very simple parameter that simply stores its value inside its definition. Nothing dynamic here - it the good old string parameter
 * directly given in a command.
 */
public class StringParameter extends Parameter
{
    private String value;

    public StringParameter(String value)
    {
        this.value = value;
    }

    @Override
    public String getValue(String replyQueueName, String prmLaunchId)
    {
        return this.value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    @Override
    public Class<? extends ParameterProvider> getProvider()
    {
        return StringParameterProvider.class;
    }
}
