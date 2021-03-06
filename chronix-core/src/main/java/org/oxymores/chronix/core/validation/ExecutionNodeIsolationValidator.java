package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.network.ExecutionNode;

public class ExecutionNodeIsolationValidator implements ConstraintValidator<ExecutionNodeIsolation, ExecutionNode>
{
    @Override
    public void initialize(ExecutionNodeIsolation constraintAnnotation)
    {
    }

    @Override
    public boolean isValid(ExecutionNode en, ConstraintValidatorContext context)
    {
        if (en == null)
        {
            return true;
        }

        if (en.getEnvironment().getNodesList().size() <= 1)
        {
            // It is normal to be isolated inside a single node network
            return true;
        }

        return en.getCanReceiveFrom().size() + en.getConnectsTo().size() > 0;
    }
}
