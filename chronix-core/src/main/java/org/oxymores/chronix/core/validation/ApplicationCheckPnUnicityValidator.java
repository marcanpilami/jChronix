package org.oxymores.chronix.core.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.Application;
import org.oxymores.chronix.core.ExecutionNode;

public class ApplicationCheckPnUnicityValidator implements ConstraintValidator<ApplicationCheckPnUnicity, Application>
{
    @Override
    public void initialize(ApplicationCheckPnUnicity constraintAnnotation)
    {}

    @Override
    public boolean isValid(Application value, ConstraintValidatorContext context)
    {
        List<String> existing = new ArrayList<String>();

        for (ExecutionNode en : value.getNodesList())
        {
            String tmp = en.getDns() + ":" + en.getqPort();
            if (existing.contains(tmp))
            {
                return false;
            }
            existing.add(tmp);
        }
        return true;
    }

}
