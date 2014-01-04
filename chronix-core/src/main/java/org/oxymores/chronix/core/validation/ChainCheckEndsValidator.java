package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;
import org.oxymores.chronix.core.active.ChainEnd;
import org.oxymores.chronix.core.active.ChainStart;

public class ChainCheckEndsValidator implements ConstraintValidator<ChainCheckEnds, Chain>
{
    @Override
    public void initialize(ChainCheckEnds constraintAnnotation)
    {}

    @Override
    public boolean isValid(Chain value, ConstraintValidatorContext context)
    {
        if (value == null)
        {
            return true;
        }

        int starts = 0, ends = 0;
        for (State s : value.getStates())
        {
            if (s.getRepresents() instanceof ChainStart)
            {
                starts++;
            }
            if (s.getRepresents() instanceof ChainEnd)
            {
                ends++;
            }
        }
        if (starts != 1 || ends != 1)
        {
            return false;
        }

        return true;
    }

}
