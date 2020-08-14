package net.ssehub.sparkyservice.api.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.LocalDate;

import javax.annotation.Nonnull;
import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.ssehub.sparkyservice.api.jpa.user.UserRealm;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;

/**
 * Validation test for {@link UserDto} and {@link UserDto.ChangePasswordDto}.
 *  
 * @author marcel
 */
public class ChangePasswordValidationTest {

    private static final String NEW_PASSWORD = "testPassword";
    private static final String OLD_PASSWORD = "oldPw123";
    private static final String USER_EMAIL = "info@test";
    private static final String PAYLOAD = "testPayload";
    private static final LocalDate EXP_DATE = LocalDate.now().plusDays(2);

    /**
     * Creates a simple and complete {@link UserDto} object for testing purposes.
     * 
     * @return complete testing dto
     */
    public static @Nonnull UserDto createExampleDto() {
        var editUserDto = new UserDto();
        editUserDto.username = "user";
        editUserDto.realm = UserRealm.UNKNOWN;
        editUserDto.passwordDto = new ChangePasswordDto();
        editUserDto.passwordDto.newPassword = NEW_PASSWORD;
        editUserDto.passwordDto.oldPassword = OLD_PASSWORD;
        editUserDto.settings = new SettingsDto();
        editUserDto.settings.payload = PAYLOAD;
        editUserDto.settings.email_address = USER_EMAIL;
        editUserDto.settings.email_receive = true;
        editUserDto.settings.wantsAi = true;
        editUserDto.expirationDate = EXP_DATE;
        return editUserDto;
    }

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
