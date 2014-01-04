package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;

public class ApplicationCheckConsoleValidator implements ConstraintValidator<ApplicationCheckConsole, Application>
{

    @Override
    public void initialize(ApplicationCheckConsole constraintAnnotation)
    {}

    @Override
    public boolean isValid(Application a, ConstraintValidatorContext context)
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
