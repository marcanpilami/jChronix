package org.oxymores.chronix.dto;

import java.io.Serializable;
import java.util.UUID;

public class DTOSequenceOccurrence implements Serializable
{
    private static final long serialVersionUID = 6619156648428643959L;

    private UUID id;
    private String label;

    public DTOSequenceOccurrence(String label)
    {
        this.label = label;
        this.id = UUID.randomUUID();
    }

    public UUID getId()
    {
        return id;
    }

    public void setId(UUID id)
    {
        this.id = id;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }
}
