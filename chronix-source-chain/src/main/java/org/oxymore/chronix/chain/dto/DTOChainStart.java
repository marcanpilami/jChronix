package org.oxymore.chronix.chain.dto;

import java.util.UUID;

import org.oxymores.chronix.core.source.api.EventSource;

public class DTOChainStart implements EventSource
{
    private static final long serialVersionUID = -5628734383363442943L;
    static final UUID START_ID = UUID.fromString("647594b0-498f-4042-933f-855682095c6c");

    @Override
    public UUID getId()
    {
        return START_ID;
    }

    @Override
    public String getName()
    {
        return "START";
    }

    @Override
    public boolean isEnabled()
    {
        return true;
    }
}
