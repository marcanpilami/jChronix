package org.oxymores.chronix.api.prm;

/**
 * The result of a parameter resolution request. Only used when resolution is done asynchronously by a plugin.
 */
public class AsyncParameterResult
{
    /**
     * The value that should be used by the engine as a parameter value.
     */
    public String result;

    /**
     * Indicates whether the parameter resolution was a success or not.
     */
    public boolean success = true;
}
