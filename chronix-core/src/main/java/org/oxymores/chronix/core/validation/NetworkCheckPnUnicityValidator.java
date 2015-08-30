package org.oxymores.chronix.core.validation;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.oxymores.chronix.core.ExecutionNode;
import org.oxymores.chronix.core.Environment;

public class NetworkCheckPnUnicityValidator implements ConstraintValidator<NetworkCheckPnUnicity, Environment>
{
    @Override
    public void initialize(NetworkCheckPnUnicity constraintAnnotation)
    {}

    @Override
    public boolean isValid(Environment value, ConstraintValidatorContext context)
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
