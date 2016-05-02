package org.oxymores.chronix.api.source;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Describes an event source instance. It is a serialisable DTO object, and considered part of the public API.
 */
public class DTOEventSource implements Serializable
{
    private static final long serialVersionUID = 7742416960083543767L;

    protected final UUID id;
    protected final String name;
    protected final String description;

    protected Map<String, DTOParameter> fields = new HashMap<>(10);
    protected List<DTOParameter> additionalParameters = new ArrayList<>(10);

    protected String behaviourClassName;
    protected transient EventSourceProvider behaviour;

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    /**
     * The only constructor. It should never be called directly - rather new instances of {@link DTOEventSource} should be obtained through
     * an OSGI service {@link EventSourceProvider#newInstance()}.
     */
    public DTOEventSource(EventSourceProvider factory, String name, String description, UUID id)
    {
        if (id == null)
        {
            this.id = UUID.randomUUID();
        }
        else
        {
            this.id = id;
        }
        this.setBehaviour(factory);
        this.name = name;
        this.description = description;
    }

    /**
     * See {@link #DTOEventSource(EventSourceProvider, String, String, UUID)} - with a null ID.
     * 
     * @param factory
     * @param name
     * @param description
     */
    public DTOEventSource(EventSourceProvider factory, String name, String description)
    {
        this(factory, name, description, null);
    }

    /**
     * Change the behaviour responsible for the handling of the event source. Can be used for changing it from one plugin to another (beware
     * of fields mapping in that case). Main use is for the engine to set the behaviour instance when deserialising the event sources from
     * file.
     */
    public void setBehaviour(EventSourceProvider factory)
    {
        this.behaviourClassName = factory.getClass().getCanonicalName();
        this.behaviour = factory;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Parameters and fields
    ///////////////////////////////////////////////////////////////////////////

    public DTOEventSource addParameter(DTOParameter prm)
    {
        if (!(this.behaviour instanceof OptionAllowsParameters))
        {
            throw new IllegalStateException("cannot add an arbitratry parameter to a type of event source which does not support it");
        }
        this.additionalParameters.add(prm);
        return this;
    }

    /**
     * Shortcut for {@link #addParameter(DTOParameter)} with a simple value parameter.
     * 
     * @param key
     * @param value
     * @return
     */
    public DTOEventSource addParameter(String key, String value)
    {
        return this.addParameter(new DTOParameter(key, value));
    }

    /**
     * Shortcut for {@link #addParameter(DTOParameter)} with a simple value parameter with a null key.
     * 
     * @param value
     * @return
     */
    public DTOEventSource addParameter(String value)
    {
        return this.addParameter(new DTOParameter(null, value));
    }

    /**
     * Set a field. The field key must be part of the keys allowed by the event source type ({@link EventSourceProvider#getFields()}).
     * Please note that as some parameter values can only be resolved at runtime, value validation does not occur when setting a parameter.
     */
    public DTOEventSource setField(DTOParameter field)
    {
        if (!(this.behaviour instanceof OptionAllowsAdditionalFields))
        {
            boolean found = false;
            for (EventSourceField f : this.behaviour.getFields())
            {
                if (f.key.equals(field.getKey()))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                throw new IllegalArgumentException(
                        field.getKey() + " is not a field expected by an event source of type " + this.behaviour.getName());
            }
        }
        this.fields.put(field.getKey(), field);
        return this;
    }

    /**
     * A shortcut for {@link #setField(DTOParameter)} with a simple direct value parameter.
     * 
     * @param key
     * @param value
     */
    public DTOEventSource setField(String key, String value)
    {
        return setField(new DTOParameter(key, value));
    }

    /**
     * Removes a field. Allowed fields key are found with {@link EventSourceProvider#getFields()}. No error is thrown if there is actually
     * no field with that key or if key is invalid.
     */
    public void removeField(String key)
    {
        this.fields.remove(key);
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

    /**
     * A short string describing the event source instance.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * The source instance name. (this is related to the instance, not to the type of event source)
     */
    public String getName()
    {
        return name;
    }

    /**
     * Every event source is described by its {@link EventSourceProvider} which gives the list of fields each event source can/should have
     * through its {@link EventSourceProvider#getFields()} method. This method returns the actual values (or means to obtain the value)
     * corresponding to said fields.<br>
     */
    public List<DTOParameter> getFields()
    {
        return new ArrayList<>(fields.values());
    }

    /**
     * Event sources may have additional parameters which are not part of the fields returned by {@link EventSourceProvider#getFields()}.
     * The typical example is shell command parameters, which are different in nature and number for each shell command and therefore cannot
     * fit inside a standardised field. This list is only used if the event source {@link EventSourceProvider#allowsArbitrayParameters()} is
     * <code>true</code><br>
     * Order inside this list is important - at runtime, parameters values are fed to the run() method of the source in this order.<br>
     * This returns a copy of the list. To actually modify the parameters use {@link #addParameter(DTOParameter)}.
     */
    public List<DTOParameter> getAdditionalParameters()
    {
        return new ArrayList<>(additionalParameters);
    }

    /**
     * The canonical name of the class implementing the {@link EventSourceProvider} OSGI service. Critical to the engine, also used by the
     * different UI.
     */
    public String getBehaviourClassName()
    {
        return behaviourClassName;
    }

    protected EventSourceProvider getBehaviour()
    {
        return this.behaviour;
    }
}
