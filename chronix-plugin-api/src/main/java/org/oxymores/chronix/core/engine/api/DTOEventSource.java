package org.oxymores.chronix.core.engine.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.core.source.api.EventSource;

public class DTOEventSource implements Serializable
{
    private static final long serialVersionUID = 6642907973807378024L;

    EventSource source;
    Map<String, Parameter> parameters = new HashMap<>();

    DTOEventSource(EventSource src)
    {
        this.setSource(src);
    }

    public DTOEventSource addParameter(String key, Parameter prm)
    {
        this.parameters.put(key, prm);
        return this;
    }

    public EventSource getSource()
    {
        return source;
    }

    protected void setSource(EventSource source)
    {
        this.source = source;
    }

    public Map<String, Parameter> getParameters()
    {
        return new HashMap<>(parameters);
    }

    protected void setParameters(Map<String, Parameter> parameters)
    {
        this.parameters = parameters;
    }
}
