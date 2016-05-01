package org.oxymores.chronix.core.transactional;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.oxymores.chronix.api.prm.AsyncParameterResult;
import org.oxymores.chronix.api.source.DTOParameter;
import org.oxymores.chronix.core.ParameterHolder;

public class ParameterResolutionRequest implements org.oxymores.chronix.api.prm.ParameterResolutionRequest
{
    private UUID id = UUID.randomUUID();
    private UUID parentSourceRequest, parentParameterRequest;

    private ParameterHolder prm;
    private final String[] additionalParameters; // Order is important - same as in parameter definition
    private final Map<String, String> fields = new HashMap<>();

    private String replayToQueueName;
    private String targetNodeName;

    private String referencedValue = null;

    public ParameterResolutionRequest(ParameterHolder prm, String replayToQueueName, String targetNodeName, UUID parentSourceRequest,
            UUID parentParameterRequest)
    {
        this.prm = prm;
        this.replayToQueueName = replayToQueueName;
        this.targetNodeName = targetNodeName;
        this.parentParameterRequest = parentParameterRequest;
        this.parentSourceRequest = parentSourceRequest;

        if (this.prm.getRawParameter().getAdditionalParameters() != null && this.prm.getRawParameter().getAdditionalParameters().size() > 0)
        {
            this.additionalParameters = new String[this.prm.getRawParameter().getAdditionalParameters().size()];
        }
        else
        {
            this.additionalParameters = null;
        }
    }

    @Override
    public UUID getRequestId()
    {
        return id;
    }

    @Override
    public DTOParameter getParameter()
    {
        return this.prm.getRawParameter();
    }

    public ParameterHolder getParameterHolder()
    {
        return this.prm;
    }

    @Override
    public List<Entry<String, String>> getAdditionalParameters()
    {
        if (this.additionalParameters == null)
        {
            throw new IllegalStateException("their are no additional parameters for a non dynamic parameter");
        }
        for (String s : this.additionalParameters)
        {
            if (s == null)
            {
                throw new IllegalStateException("the parameters of this parameter are not fully resolved yet");
            }
        }
        int i = 0;
        List<Entry<String, String>> res = new ArrayList<>();
        for (DTOParameter prm : this.prm.getRawParameter().getAdditionalParameters())
        {
            res.add(new AbstractMap.SimpleEntry<String, String>(prm.getKey(), this.additionalParameters[i]));
            i++;
        }
        return res;
    }

    @Override
    public Map<String, String> getFields()
    {
        return new HashMap<>(this.fields);
    }

    @Override
    public String getReplyToQueueName()
    {
        return this.replayToQueueName;
    }

    @Override
    public String getNodeName()
    {
        return this.targetNodeName;
    }

    /**
     * Is this parameter request ready for evaluation?
     */
    public boolean isReady()
    {
        return this.prm.getRawParameter().getDirectValue() != null
                || (this.prm.getRawParameter().getReference() != null && this.prm.getRawParameter().getProviderName() == null)
                || (this.prm.getRawParameter().getProviderName() != null
                        && resolvedAdditionalPrm() == this.prm.getRawParameter().getAdditionalParameters().size()
                        && this.fields.size() == this.prm.getRawParameter().getFields().size());
    }

    private int resolvedAdditionalPrm()
    {
        int res = 0;
        for (int i = 0; i < this.additionalParameters.length; i++)
        {
            if (this.additionalParameters[i] != null)
            {
                res++;
            }
        }
        return res;
    }

    /**
     * Is this parameter request for a reference?
     */
    public boolean isReference()
    {
        return this.prm.getRawParameter().getDirectValue() == null && this.prm.getRawParameter().getProviderName() == null
                && this.prm.getRawParameter().getReference() != null;
    }

    public boolean isDynamic()
    {
        return !this.isReference() && this.prm.getRawParameter().getDirectValue() == null;
    }

    /**
     * The parameter is actually a redirection to another parameter designated by this ID. Null if not a redirection.
     */
    public UUID getReference()
    {
        return this.prm.getRawParameter().getReference();
    }

    /**
     * For dynamically resolved parameters only. Sets the result of the resolution of a field.
     */
    public void setFieldValue(AsyncParameterResult res, ParameterResolutionRequest rq)
    {
        if (this.prm.getRawParameter().getProviderName() == null)
        {
            throw new IllegalStateException("cannot resolve fields for a non dynamic parameter");
        }

        DTOParameter targetPrm = this.prm.getRawParameter().getFields().get(rq.getParameter().getKey());
        if (targetPrm == null)
        {
            throw new IllegalArgumentException(
                    "this parameter request is not waiting for a field value with key " + rq.getParameter().getKey());
        }

        this.fields.put(rq.getParameter().getKey(), res.result);
    }

    public void setPrmValue(UUID dtoPrmId, String childValue)
    {
        // Value can be either a field or an additional parameter.
        for (DTOParameter field : this.prm.getRawParameter().getFields().values())
        {
            if (field.getId().equals(dtoPrmId))
            {
                this.fields.put(field.getKey(), childValue);
                return;
            }
        }

        int i = 0;
        for (DTOParameter field : this.prm.getRawParameter().getAdditionalParameters())
        {
            if (field.getId().equals(dtoPrmId))
            {
                this.additionalParameters[i] = childValue;
                return;
            }
            i++;
        }
    }

    public UUID getParentParameterRequest()
    {
        return parentParameterRequest;
    }

    public UUID getParentSourceRequest()
    {
        return parentSourceRequest;
    }

    public String getDirectValue()
    {
        return this.prm.getRawParameter().getDirectValue();
    }

    public String getNonDynamicValue()
    {
        if (this.prm.getRawParameter().getDirectValue() != null)
        {
            return this.prm.getRawParameter().getDirectValue();
        }
        if (this.referencedValue != null)
        {
            return this.referencedValue;
        }
        return null;
    }
}
