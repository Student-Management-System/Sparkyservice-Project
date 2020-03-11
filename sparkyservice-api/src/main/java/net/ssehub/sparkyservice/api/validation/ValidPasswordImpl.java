package net.ssehub.sparkyservice.api.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidPasswordImpl implements ConstraintValidator<ValidPassword, String>{

    @Override
    public boolean isValid(String rawPassword, ConstraintValidatorContext arg1) {
        boolean passwordLengthOk = rawPassword.length() > 5;
        return passwordLengthOk;
    }

}
