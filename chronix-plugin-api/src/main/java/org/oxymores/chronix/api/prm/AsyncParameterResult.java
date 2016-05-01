package org.oxymores.chronix.api.prm;

import java.io.Serializable;
import java.util.UUID;

/**
 * The result of a parameter resolution request. Only used when resolution is done asynchronously by a plugin.
 */
public class AsyncParameterResult implements Serializable
{
    private static final long serialVersionUID = -4874176411989663535L;

    /**
     * The request ID as specified inside {@link ParameterResolutionRequest}. This allows the engine to link the result to the request.
     */
    public UUID requestId;

    /**
     * The value that should be used by the engine as a parameter value. Can be an empty string, but not null (except if success is false).
     */
    public String result;

    /**
     * Indicates whether the parameter resolution was a success or not.
     */
    public boolean success = true;
}
