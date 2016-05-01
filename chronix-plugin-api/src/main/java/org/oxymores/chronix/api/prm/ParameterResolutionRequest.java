package org.oxymores.chronix.api.prm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.oxymores.chronix.api.source.DTOParameter;

/**
 * A bag containing everything a parameter plugin needs in order to return an actual string value to the engine "resolving a parameter").
 * The main parameter of {@link ParameterProvider#getValue(ParameterJobDescription)}
 *
 */
public interface ParameterResolutionRequest
{
    /**
     * An identifier for the resolution request. Used in asynchronous responses.
     */
    public UUID getRequestId();

    /**
     * The parameter to resolve. Given more as a helper reference than anything - the actual values to use for fields and parameters are
     * given by other methods of this object, and the Parameter object only contains raw values or references.
     */
    public DTOParameter getParameter();

    /**
     * Every parameter type is associated to an optional (sorted) set of parameters. What to do with them is left to the parameter plugin.
     * For example, the shell command plugin will use them as arguments to the shell command line. These can use interpolation (use of other
     * variables or parameters) or be the result of another job - they are presented here fully resolved.<br>
     * The order of the list may be important to respect depending on the plugin.<br>
     * Note that this is an <code>Entry</code> list and not a Map: keys can be empty or non-unique.
     */
    public List<Map.Entry<String, String>> getAdditionalParameters();

    /**
     * The resolved values corresponding to the fields of the parameter as described by {@link ParameterProvider#getFields()} <br>
     * The values have already been validated.<br>
     * This is a <code>Map</code> as field keys are unique and order is not important (fields should always be accessed by key).
     */
    public Map<String, String> getFields();

    /**
     * In case the parameter is not resolved synchronously but asynchronously, this returns the name of the JMS queue that should be used to
     * send the result to (in the form of a {@link AsyncParameterResult}).
     */
    public String getReplyToQueueName();

    /**
     * The name of the node associated to the current call stack.
     */
    public String getNodeName();
}
