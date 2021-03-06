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
import org.oxymores.chronix.core.app.ParameterDef;

/**
 * Represents an ongoing parameter resolution request. Basically, the equivalent of PipelineJob for parameters. Note that as parameter
 * resolutions are always idempotent, we do not need to persist this object after a crash, we simply create new requests.
 */
public class ParameterResolutionRequest implements org.oxymores.chronix.api.prm.ParameterResolutionRequest
{
    private UUID id = UUID.randomUUID();
    private UUID parentSourceRequest, parentParameterRequest;

    private ParameterDef prm;
    private final String[] additionalParameters; // Order is important - same as in parameter definition
    private final Map<String, String> fields = new HashMap<>();

    private String replyToQueueName;
    private String targetNodeName;

    private String referencedValue = null;

    public ParameterResolutionRequest(ParameterDef prm, String replyToQueueName, String targetNodeName, UUID parentSourceRequest,
            UUID parentParameterRequest)
    {
        this.prm = prm;
        this.replyToQueueName = replyToQueueName;
        this.targetNodeName = targetNodeName;
        this.parentParameterRequest = parentParameterRequest;
        this.parentSourceRequest = parentSourceRequest;

        if (this.prm.getAdditionalParameters() != null && this.prm.getAdditionalParameters().size() > 0)
        {
            this.additionalParameters = new String[this.prm.getAdditionalParameters().size()];
        }
        else
        {
            this.additionalParameters = new String[0];
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
        return this.prm.getDTO();
    }

    public UUID getParameterId()
    {
        return this.prm.getParameterId();
    }

    public ParameterDef getParameterDef()
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
        for (ParameterDef prm : this.prm.getAdditionalParameters())
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
        return this.replyToQueueName;
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
        return this.prm.getDirectValue() != null
                || (this.prm.getReference() != null && this.referencedValue != null && this.prm.getProviderClassName() == null)
                || (this.prm.getProviderClassName() != null && resolvedAdditionalPrm() == this.prm.getAdditionalParameters().size()
                        && this.fields.size() == this.prm.getFields().size());
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
        return this.prm.getDirectValue() == null && this.prm.getProviderClassName() == null && this.prm.getReference() != null;
    }

    public boolean isDynamic()
    {
        return !this.isReference() && this.prm.getDirectValue() == null;
    }

    /**
     * The parameter is actually a redirection to another parameter designated by this ID. Null if not a redirection.
     */
    public UUID getReference()
    {
        return this.prm.getReference();
    }

    public String getReferencedValue()
    {
        return this.referencedValue;
    }

    /**
     * For dynamically resolved parameters only. Sets the result of the resolution of a field.
     * 
     * @param rq
     *            the request made for a parameter of this parameter. (field or additional)
     */
    public void setFieldOrParamValue(AsyncParameterResult res, ParameterResolutionRequest rq)
    {
        if (this.isReference())
        {
            this.referencedValue = res.result;
            return;
        }

        if (this.prm.getProviderClassName() == null)
        {
            throw new IllegalStateException("cannot resolve fields for a non dynamic parameter");
        }

        ParameterDef targetPrm = this.prm.getField(rq.getParameter().getId());
        if (targetPrm != null)
        {
            this.fields.put(rq.getParameter().getKey(), res.result);
            return;
        }

        targetPrm = this.prm.getAdditionalParameter(rq.getParameter().getId());
        if (targetPrm != null)
        {
            int i = 0;
            for (ParameterDef prm : this.prm.getAdditionalParameters())
            {
                if (prm.getParameterId().equals(rq.getParameter().getId()))
                {
                    break;
                }
                i++;
            }
            this.additionalParameters[i] = res.result;
            return;
        }

        throw new IllegalArgumentException(
                "this parameter request is not waiting for a field value with key " + rq.getParameter().getKey());
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
        return this.prm.getDirectValue();
    }
}
