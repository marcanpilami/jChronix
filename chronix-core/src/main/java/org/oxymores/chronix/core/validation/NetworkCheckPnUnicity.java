package org.oxymores.chronix.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Documented
@Constraint(validatedBy = NetworkCheckPnUnicityValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NetworkCheckPnUnicity
{
    String message() default "the network contains multiple engines with the same dns and port";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
