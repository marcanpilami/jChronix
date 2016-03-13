package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymore.chronix.source.PlanBehaviour;
import org.oxymores.chronix.core.source.api.EngineCallback;
import org.oxymores.chronix.core.source.api.EventSourceContainer;
import org.oxymores.chronix.core.source.api.EventSourceOptionNoState;
import org.oxymores.chronix.core.source.api.EventSourceProvider;
import org.oxymores.chronix.core.source.api.EventSourceRunResult;
import org.oxymores.chronix.core.source.api.JobDescription;

public class DTOPlan extends EventSourceContainer implements EventSourceOptionNoState
{
    private static final long serialVersionUID = -5008049889665310849L;

    public DTOPlan()
    {
        // For serialisation
    }

    /**
     * Helper constructor that creates a ready to use plan.
     */
    public DTOPlan(String name, String description)
    {
        this.name = name;
        this.description = description;
        this.id = UUID.randomUUID();
    }

    ///////////////////////////////////////////////////////////////////////////
    // RUN
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public EventSourceRunResult run(EngineCallback cb, JobDescription jd)
    {
        throw new IllegalStateException("A plan itself cannot be launched. It contains elements that launch themselves");
    }

    ///////////////////////////////////////////////////////////////////////////
    // HELPERS
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public Class<? extends EventSourceProvider> getProvider()
    {
        return PlanBehaviour.class;
    }
}
