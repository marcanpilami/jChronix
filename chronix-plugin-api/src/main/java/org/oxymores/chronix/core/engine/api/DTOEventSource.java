package org.oxymores.chronix.core.engine.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.core.source.api.EventSource;

public class DTOEventSource implements Serializable
{
    private static final long serialVersionUID = 6642907973807378024L;

    EventSource source;
    List<Parameter> parameters = new ArrayList<>();

    DTOEventSource(EventSource src)
    {
        this.setSource(src);
    }

    public void addParameter(Parameter prm)
    {
        this.parameters.add(prm);
    }

    public EventSource getSource()
    {
        return source;
    }

    protected void setSource(EventSource source)
    {
        this.source = source;
    }

    public List<Parameter> getParameters()
    {
        return new ArrayList<>(parameters);
    }

    protected void setParameters(List<Parameter> parameters)
    {
        this.parameters = parameters;
    }
}
