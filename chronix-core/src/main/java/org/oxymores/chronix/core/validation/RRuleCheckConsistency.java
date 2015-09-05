package org.oxymores.chronix.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = RRuleCheckConsistencyValidator.class)
@Target(
        {
            ElementType.TYPE
        })
@Retention(RetentionPolicy.RUNTIME)
public @interface RRuleCheckConsistency
{
    String message() default "RRule [${validatedValue}] is using a restriction with a finer grain than its period, or its period is invalid";

    Class<?>[] groups() default
    {
    };

    Class<? extends Payload>[] payload() default
    {
    };
}
