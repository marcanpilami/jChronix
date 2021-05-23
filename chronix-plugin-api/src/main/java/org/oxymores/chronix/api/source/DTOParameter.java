package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.oxymores.chronix.api.prm.ParameterProvider;

/**
 * DTO for all parameter-like objects in the API:
 * <ul>
 * <li><strong>event source fields</strong>: elements fully described and expected by the event source</li>
 * <li><strong>event source additional parameters</strong>: arbitrary elements that can be added to further characterise an event source,
 * such as shell command line parameters</li>
 * <li>as well as their equivalents for dynamic parameters (since a parameter can be dynamically resolved... and itself have parameters)
 * </li><br>
 * </ul>
 * Parameters can be of three types, all described with this class:
 * <ul>
 * <li><strong>direct value</strong>: in this case, this object simply contains a String representing the desired value.</li>
 * <li><strong>dynamically resolved</strong>: a {@link ParameterProvider} will resolve the actual String value at runtime according to a
 * list of parameters contained by this class.</li>
 * <li><strong>reference</strong>: a reference to another {@link DTOParameter} (of any type, but mostly used for dynamically resolved
 * parameters) to allow the mutualisation of complex parameters</li>
 * </ul>
 * <br>
 * All parameter types can result in String values containing the pattern %{[a-zA-Z0-9]+}. These will be replaced at runtime by the
 * environment value of the same name. Escape character is \ (${myvarname} will be replaced but not \%{myvarname}).<br>
 * <br>
 * The different parameter types are described in this unique class and not in specialised subclasses (which would be a more Java-ish way of
 * doing things) as this is a DTO class and therefore subject to different methods of deserialisation - and deserialisation mechanisms are
 * often at odds with inheritance.<br>
 * In case a DTO instance contains values for multiple types, the order of precedence is direct > dynamic > reference.
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DTOParameter implements Serializable
{
    private static final long serialVersionUID = -6464225583623579497L;

    // Common
    private UUID id = UUID.randomUUID();
    private String key;

    // Direct value
    private String value;

    // Reference
    private UUID prmReference = null;

    // Dynamically resolved
    private String providerClassName;
    private Map<String, DTOParameter> fields = null;
    private List<DTOParameter> additionalParameters = null;

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    protected DTOParameter()
    {
        // JB convention
    }

    /**
     * Creates a direct value parameter - that is a parameter directly containing a string value, with no need for dynamic resolution.
     * 
     * @param key
     *            the key to use, can be null for optional parameters.
     * @param value
     *            the value to return when resolving the parameter.
     */
    public DTOParameter(String key, String value)
    {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a reference type parameter - that is a parameter that is a simple reference to another parameter.
     * 
     * @param key
     *            the key to use, can be null for optional parameters.
     * @param referencedParameterId
     *            the ID of the {@link DTOParameter} which will actually provide the parameter value.
     */
    public DTOParameter(String key, UUID referencedParameterId)
    {
        this.key = key;
        this.prmReference = referencedParameterId;
    }

    /**
     * Creates a dynamically resolved parameter. This constructor should not be called directly - it is better to use the factory methods of
     * parameter providers.
     * 
     * @param key
     * @param prv
     */
    public DTOParameter(String key, ParameterProvider prv)
    {
        this.key = key;
        this.providerClassName = prv.getClass().getCanonicalName();
        this.fields = new HashMap<>(10);
        this.additionalParameters = new ArrayList<>(10);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The unique ID.
     */
    public UUID getId()
    {
        return id;
    }

    public DTOParameter setId(UUID id)
    {
        this.id = id;
        return this;
    }

    /**
     * This key is either the name of a field (in which case it must be non-null and non-empty) or the "modifier" for an optional argument
     * (for example, the name "--file" for a shell command argument). In this latter case, it can be null or empty (ignored in both cases).
     * <br>
     * For reference parameters, this always (even if null or empty) replaces the (possible) key of the referenced parameter.
     */
    public String getKey()
    {
        return key;
    }

    /**
     * The value of the parameter. Only defined if this is a <strong>direct value</strong> parameter. If defined, it wins over all other
     * methods of obtaining a parameter value.
     */
    public String getDirectValue()
    {
        return this.value;
    }

    /**
     * The ID of the parameter to actually use (redirection). Only defined if this is a <strong>reference</strong>. If
     * {@link #getDirectValue()} is non null, this is ignored. If {@link #getProviderName()} is non null, this is ignored.
     */
    public UUID getReference()
    {
        return this.prmReference;
    }

    /**
     * The name of the provider responsible for dynamically providing a value at runtime. Only defined if this is a <strong>dynamically
     * resolved</strong> parameter. If {@link #getDirectValue()} is non null, this is ignored.
     */
    public String getProviderName()
    {
        return this.providerClassName;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Dynamic elements handling
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sets a field. The field key must be part of the keys allowed by the parameter type ({@link ParameterProvider#getFields()}). Please
     * note that as some parameter values can only be resolved at runtime, no validation occurs when setting a field.<br>
     * Fields can be set in any order. Key cannot be null.
     */
    public DTOParameter setField(DTOParameter field)
    {
        if (this.fields == null)
        {
            throw new IllegalStateException("parameter is not a dynamically resolved parameter");
        }
        if (field.getKey() == null)
        {
            throw new IllegalArgumentException("the key of a field cannot be null");
        }
        this.fields.put(field.getKey(), field);
        return this;
    }

    /**
     * Shortcut for {@link #setField(String, DTOParameter)} with a simple value parameter.
     * 
     * @param key
     * @param value
     * @return
     */
    public DTOParameter setField(String key, String value)
    {
        return this.setField(new DTOParameter(key, value));
    }

    /**
     * @return a copy of the fields used for dynamic resolution.
     */
    public Map<String, DTOParameter> getFields()
    {
        return this.fields == null ? null : new HashMap<>(this.fields);
    }

    /**
     * Adds an optional parameter. Order is important.
     * 
     * @param prm
     *            the parameter to add to the list of optional parameters.
     */
    public DTOParameter addAdditionalarameter(DTOParameter prm)
    {
        if (this.additionalParameters == null)
        {
            throw new IllegalStateException("parameter is not a dynamically resolved parameter");
        }
        this.additionalParameters.add(prm);
        return this;
    }

    public DTOParameter addAdditionalParameter(String stringValue)
    {
        if (this.additionalParameters == null)
        {
            throw new IllegalStateException("parameter is not a dynamically resolved parameter");
        }
        this.additionalParameters.add(new DTOParameter(null, stringValue));
        return this;
    }

    public DTOParameter addAdditionalarameter(String key, String stringValue)
    {
        if (this.additionalParameters == null)
        {
            throw new IllegalStateException("parameter is not a dynamically resolved parameter");
        }
        this.additionalParameters.add(new DTOParameter(key, stringValue));
        return this;
    }

    /**
     * @return a list of all additional parameters. Order may be important depending on the parameter plugin.
     */
    public List<DTOParameter> getAdditionalParameters()
    {
        return this.additionalParameters == null ? null : new ArrayList<>(this.additionalParameters);
    }
}
