package org.oxymores.chronix.core.source.api;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public interface DTO extends Serializable
{
    /**
     * Every event source instance has an unique ID. We use statistically unique UUID since this is an easy solution for uniqueness in
     * distributed systems like Chronix.
     */
    public UUID getId();

    /**
     * A name, less than 20 characters.
     */
    public String getName();

    /**
     * Event sources can be disabled by returning false. In this case, every State that uses this sources will run in disabled mode (i.e.
     * the {@link EventSourceBehaviour#runDisabled(EngineCallback, JobDescription)} will be called instead of calling
     * {@link EventSourceBehaviour#run(EngineCallback, JobDescription)}.
     */
    public boolean isEnabled();
}
