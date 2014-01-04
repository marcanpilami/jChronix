package org.oxymores.chronix.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ChainCheckCycleValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ChainCheckCycle
{
    String message() default "Chain [${validatedValue}] has at least one cycle";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
