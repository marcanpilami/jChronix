package org.oxymores.chronix.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = ApplicationCheckConsoleValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationCheckConsole
{
    String message() default "an application must have one and only one console in its physical network";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
