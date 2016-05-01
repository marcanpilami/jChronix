package org.oxymores.chronix.core;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.oxymores.chronix.api.prm.ParameterResolutionRequest;
import org.oxymores.chronix.api.source.DTOParameter;

public class ParameterRequest implements ParameterResolutionRequest
{
    private UUID id, parentId;
    private DTOParameter prm;

    @Override
    public UUID getRequestId()
    {
        return this.id;
    }

    @Override
    public DTOParameter getParameter()
    {
        return prm;
    }

    @Override
    public List<Entry<String, String>> getAdditionalParameters()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String> getFields()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getReplyToQueueName()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNodeName()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
