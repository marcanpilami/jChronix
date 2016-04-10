package org.oxymores.chronix.api.source2;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of one field of an event source.(meta)
 *
 */
public class EventSourceField
{
    /**
     * The technical identifier of the field. Unique (within an event source) and non null and non empty. Non-translatable.
     */
    public String key;

    /**
     * The name that should be used in user interfaces. Less than 255 characters. Translatable.
     */
    public String prettyName;

    /**
     * Can be null. Default is null. Used by user interfaces.
     */
    public String defaultValue = null;

    /**
     * An event source is not valid if this field is absent. (can still be null)
     */
    public boolean compulsory = true;

    /**
     * If the field is present, it will be submitted to these validators. List can be empty, but in that case no validation will take place.
     */
    public List<EventSourceFieldValidator> validators = new ArrayList<>();

    public EventSourceField(String key, String prettyName, String defaultValue, boolean compulsory)
    {
        this.key = key;
        this.prettyName = prettyName;
        this.defaultValue = defaultValue;
        this.compulsory = compulsory;
    }
}
