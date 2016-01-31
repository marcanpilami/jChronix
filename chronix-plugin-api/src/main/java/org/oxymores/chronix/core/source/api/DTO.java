package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.UUID;

public interface DTO extends Serializable
{
    public UUID getId();

    public String getName();
}
