package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.oxymores.chronix.core.Environment;
import org.oxymores.chronix.core.network.ExecutionNode;

public class NetworkCheckConsoleValidator implements ConstraintValidator<NetworkCheckConsole, Environment>
{
    @Override
    public void initialize(NetworkCheckConsole constraintAnnotation)
    {
    }

    @Override
    public boolean isValid(Environment a, ConstraintValidatorContext context)
    {
        if (a == null)
        {
            return true;
        }
        if (a.getNodes().isEmpty())
        {
            return false;
        }
        int nbComputingNodes = 0;
        for (ExecutionNode en : a.getNodesList())
        {
            if (!en.isHosted())
            {
                nbComputingNodes++;
            }
        }

        return (nbComputingNodes == 1 && a.getConsole() == null) || (nbComputingNodes > 1 && a.getConsole() != null);
    }
}
