package org.oxymores.chronix.core.validation;

import java.util.ArrayList;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.Chain;
import org.oxymores.chronix.core.State;

public class ChainCheckCycleValidator implements ConstraintValidator<ChainCheckCycle, Chain>
{

    @Override
    public void initialize(ChainCheckCycle constraintAnnotation)
    {}

    @Override
    public boolean isValid(Chain c, ConstraintValidatorContext context)
    {
        if (c == null || c.getStartState() == null || c.getEndState() == null)
        {
            return true;
        }

        return !hasCycle(c.getStartState(), new ArrayList<State>());
    }

    private boolean hasCycle(State s, ArrayList<State> alreadyDone)
    {
        if (alreadyDone.contains(s))
        {
            return true;
        }
        @SuppressWarnings("unchecked")
        ArrayList<State> ad = (ArrayList<State>) alreadyDone.clone();
        ad.add(s);

        for (State dest : s.getClientStates())
        {
            if (hasCycle(dest, ad))
            {
                return true;
            }
        }

        return false;
    }
}
