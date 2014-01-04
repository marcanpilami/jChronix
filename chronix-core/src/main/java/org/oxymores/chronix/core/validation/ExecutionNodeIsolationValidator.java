package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.ExecutionNode;

public class ExecutionNodeIsolationValidator implements ConstraintValidator<ExecutionNodeIsolation, ExecutionNode>
{
    @Override
    public void initialize(ExecutionNodeIsolation constraintAnnotation)
    {}

    @Override
    public boolean isValid(ExecutionNode en, ConstraintValidatorContext context)
    {
        if (en == null)
        {
            return true;
        }

        if (en.getApplication().getNodesList().size() <= 1)
        {
            // It is normal to be isolated inside a single node network
            return true;
        }

        if (en.getCanReceiveFrom().size() + en.getCanSendTo().size() <= 0)
        {
            return false;
        }

        return true;
    }

}
