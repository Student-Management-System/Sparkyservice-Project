package net.ssehub.sparkyservice.api.useraccess.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Provides an annotation for runtime password validation. Passwords must met requirements to pass the validation.
 * 
 * @author marcel
 */
@Target( {ElementType.FIELD, ElementType.METHOD})
@Retention( RetentionPolicy.RUNTIME)
@Constraint( validatedBy = ValidPasswordImpl.class)
public @interface ValidPassword {

    String message() default "Password is shorter than 5 characters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}