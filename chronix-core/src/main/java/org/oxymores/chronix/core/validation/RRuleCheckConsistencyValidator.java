package org.oxymores.chronix.core.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.oxymores.chronix.core.active.ClockRRule;

public class RRuleCheckConsistencyValidator implements ConstraintValidator<RRuleCheckConsistency, ClockRRule>
{
    @Override
    public void initialize(RRuleCheckConsistency constraintAnnotation)
    {
    }

    @Override
    public boolean isValid(ClockRRule value, ConstraintValidatorContext context)
    {
        switch (value.getPeriod())
        {
            case "MINUTELY":
                return value.getBYHOUR().isEmpty() && value.getBYDAY().isEmpty() && value.getBYMONTHDAY().isEmpty()
                        && value.getBYMONTH().isEmpty() && value.getBYYEAR().isEmpty();
            case "HOURLY":
                return value.getBYDAY().isEmpty() && value.getBYMONTHDAY().isEmpty()
                        && value.getBYMONTH().isEmpty() && value.getBYYEAR().isEmpty();
            case "DAILY":
                return value.getBYMONTH().isEmpty() && value.getBYYEAR().isEmpty();
            case "WEEKLY":
                return value.getBYMONTH().isEmpty() && value.getBYYEAR().isEmpty();
            case "MONTHLY":
                return value.getBYYEAR().isEmpty();
            case "YEARLY":
                return true;
            default:
                return false;
        }
    }

}
