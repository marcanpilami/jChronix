package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.DTO;

public class DTOChainEnd implements DTO
{
    private static final long serialVersionUID = -3859771632110912194L;
    static final UUID END_ID = UUID.fromString("8235272c-b78d-4350-a887-aed0dcdfb215");

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

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
