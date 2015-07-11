package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Network;

public class NetworkCheckConsoleValidator implements ConstraintValidator<NetworkCheckConsole, Network>
{

    @Override
    public void initialize(NetworkCheckConsole constraintAnnotation)
    {}

    @Override
    public boolean isValid(Network a, ConstraintValidatorContext context)
    {
        if (a == null)
        {
            return true;
        }

        int i = 0;
        for (ExecutionNode en : a.getNodesList())
        {
            if (en.isConsole())
            {
                i++;
            }
        }

        if (i == 1)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

}
