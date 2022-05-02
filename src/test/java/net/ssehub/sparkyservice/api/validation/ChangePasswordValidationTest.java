package net.ssehub.sparkyservice.api.validation;

import static net.ssehub.sparkyservice.api.testconf.TestSetupMethods.createExampleDto;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

/**
 * Validation test for {@link UserDto} and {@link UserDto.ChangePasswordDto}.
 *  
 * @author marcel
 */
public class ChangePasswordValidationTest {

    /**
     * Tests if the {@link #createExampleDto()} returns a valid dto object.<br>
     * This is a test case for {@link NotNull} and other validation annotations.
     */
    @Test
    public void editUserDtoValidationTest() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        var violations = validator.validate(userDto);
        assertTrue(violations.isEmpty(), "The given value didn't pass the validity test");
    }

    /**
     * Test for {@link UserDto#username} validation. Tries different input which the
     * validator should not pass.
     * 
     * @param username - A username to test
     */
    @ParameterizedTest
    @ValueSource(strings = { " ", "", "null" })
    public void editDtoValidationUsernameTest(String username) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        assumeTrue(validator.validate(userDto).isEmpty(), "The provided example dto is not correct. Skip this test");
        userDto.username = "null".equals(username) ? null : username;
        assertFalse(validator.validate(userDto).isEmpty(), "The validator pass invalid values for username.");
    }

    /**
     * Test for {@link UserDto.ChangePasswordDto#newPassword} validation. Tries
     * different input which the validator should not pass.
     * 
     * @param password - A password to test
     */
    @ParameterizedTest
    @ValueSource(strings = { " ", "", "null" })
    public void editDtoValidationPasswordTest(String password) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        assumeTrue(validator.validate(userDto).isEmpty(), "The provided example dto is not correct. Skip this test");
        userDto.passwordDto.newPassword = "null".equals(password) ? null : password;
        assertFalse(validator.validate(userDto).isEmpty(), "The validator pass invalid values for username.");
    }
}
