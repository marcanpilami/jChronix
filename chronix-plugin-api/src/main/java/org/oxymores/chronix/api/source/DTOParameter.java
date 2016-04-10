package org.oxymores.chronix.api.source;

import java.util.UUID;

import org.oxymores.chronix.api.prm.Parameter;
import org.oxymores.chronix.core.engine.api.DTOApplication;

/**
 * Container for both <strong>event source fields</strong> (that is elements fully described and expected by the event source) and
 * <strong>event source additional parameters</strong> (arbitrary elements that can be added to further characterise an event source, such
 * as shell command line parameters).<br>
 * Parameters can be either directly described through an {@link Parameter} element contained in the {@link DTOParameter} or be a reference
 * to another shared {@link DTOParameter}. If both direct value and reference are defined, the object is invalid and an error is raised.
 * 
 */
public class DTOParameter
{
    private UUID id = UUID.randomUUID();
    private String key;
    private Parameter prm = null;
    private UUID prmReference = null;

    /**
     * The unique ID.
     */
    public UUID getId()
    {
        return id;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////////////////////////

    public DTOParameter(String key, Parameter prm)
    {
        this.key = key;
        this.prm = prm;
    }

    public DTOParameter(String key, UUID referencedParameterId)
    {
        this.key = key;
        this.prmReference = referencedParameterId;
    }

    ///////////////////////////////////////////////////////////////////////////
    // Stupid accessors
    ///////////////////////////////////////////////////////////////////////////

    /**
     * This key is either the name of a field (in which case it non-null and non-empty) or the "modifier" for an optional argument (for
     * example, the name "--file" for a shell command argument). In this latter case, it can be null or empty.
     */
    public String getKey()
    {
        return key;
    }

    /**
     * This returns the {@link Parameter} behind this object (be it a parameter local to the event source or a reference to a shared
     * Parameter).
     */
    public Parameter getPrm(DTOApplication app)
    {
        if (this.prm != null && this.prmReference != null)
        {
            throw new IllegalStateException(
                    "invalid parameter reference - cannot be both a local parameter and a shared parameter reference");
        }
        if (this.prm != null)
        {
            return prm;
        }
        else
        {
            return app.getSharedParameter(this.prmReference).getPrm(app);
        }
    }
}
