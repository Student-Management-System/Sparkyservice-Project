package net.ssehub.sparkyservice.api.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public final class ValidPasswordImpl implements ConstraintValidator<ValidPassword, String>{

    @Override
    public boolean isValid(String rawPassword, ConstraintValidatorContext arg1) {
        boolean passwordLengthOk = rawPassword != null && !rawPassword.isBlank() && rawPassword.length() > 5;
        return passwordLengthOk;
    }

}
