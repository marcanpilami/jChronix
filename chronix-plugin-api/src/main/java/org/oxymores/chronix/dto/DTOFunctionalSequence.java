package org.oxymores.chronix.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DTOFunctionalSequence implements Serializable
{
    private static final long serialVersionUID = -7441844678474104466L;

    private UUID id;
    private String name, description;
    private int alertThreshold = 0;
    private List<DTOSequenceOccurrence> days = new ArrayList<>();

    public DTOFunctionalSequence(String name, String description)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();
    }

    /**
     * The unique ID of the object.
     */
    public UUID getId()
    {
        return id;
    }

    /**
     * Should only be used by the engine.
     */
    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    // for serialisation only.
    void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    // for serialisation only.
    void setDescription(String description)
    {
        this.description = description;
    }

    public int getAlertThreshold()
    {
        return alertThreshold;
    }

    public DTOFunctionalSequence setAlertThreshold(int alertThreshold)
    {
        this.alertThreshold = alertThreshold;
        return this;
    }

    /**
     * A copy of the list of all occurrences belonging to this sequence. Modifying the result of this method does not modify the sequence
     * itself.
     */
    public List<DTOSequenceOccurrence> getDays()
    {
        return new ArrayList<>(days);
    }

    // for serialisation only.
    void setDays(List<DTOSequenceOccurrence> days)
    {
        this.days = days;
    }

    /**
     * Adds a new occurrence at the end of the list.
     */
    public DTOFunctionalSequence addOccurrence(String label)
    {
        this.days.add(new DTOSequenceOccurrence(label));
        return this;
    }
}
