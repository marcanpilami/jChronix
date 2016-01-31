package org.oxymore.chronix.chain.dto;

import java.io.Serializable;
import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTO;

public class DTOChainEnd implements DTO, Serializable
{
    private static final long serialVersionUID = -3859771632110912194L;
    private static final UUID END_ID = UUID.fromString("8235272c-b78d-4350-a887-aed0dcdfb215");

    @Override
    public UUID getId()
    {
        return END_ID;
    }

    @Override
    public String getName()
    {
        return "END";
    }
}
