package net.ssehub.sparkyservice.api.user.dto;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.annotation.Nonnull;
import javax.validation.Validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;

public class NewUserDtoTests {

    /**
     * @return valid DTO object. 
     */
    @Test
    private @Nonnull NewUserDto createExampleDto() {
        var newUserDto = new NewUserDto();
        newUserDto.username = "user";
        newUserDto.role = UserRole.ADMIN;
        newUserDto.password = "hallo123";
        return newUserDto;
    }

    /**
     * Tests validation for a simple DTO. Tests {@link #createExampleDto()}. 
     */
    @Test
    public void newUserValidationTest() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        var violations = validator.validate(userDto);
        assertTrue(violations.isEmpty(), 
                "The given value didn't pass the validity test, Violation: " + violations.toString());
    }

    /**
     * Test validation for {@link NewUserDto#password} with different input which aren't valid.
     * 
     * @param password the password input to check
     */
    @ParameterizedTest
    @ValueSource(strings = {" ", "", "null"})
    public void newUserPasswordValidationTest(String password) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        assumeTrue(validator.validate(userDto).isEmpty(), "The provided example dto is not correct. Skip this test");
        userDto.password = "null".equals(password) ? null : password;
        assertFalse(validator.validate(userDto).isEmpty(), "Password is not validated. Wrong values passes the test.");
    }

    /**
     * Tests for {@link NewUserDto#transformToUser(NewUserDto)}. <br>
     * Transform a DTO to a {@link LocalUserDetails} and checks if each value is transformed. <br>
     * Does not check the transformation of {@link NewUserDto#getPersonalSettings()}. This is done in another test.
     */
    @Test
    public void transformToUserTest() {
        var userDto = createExampleDto();
        userDto.setPersonalSettings(new SettingsDto());
        LocalUserDetails details = NewUserDto.transformToUser(userDto);
        assertAll( 
            () -> assertEquals(userDto.username, details.getUsername()),
            () -> assertEquals(userDto.role, details.getRole()),
            () -> assertNotNull(details.getPasswordEntity())
        );
    }

    /**
     * Tries to perform a transformation on an invalid DTO object. 
     */
    @Test
    public void transformNullUserValuesTest() {
        var userDto = new NewUserDto();
        assertThrows(IllegalArgumentException.class, 
                () -> NewUserDto.transformToUser(userDto));
    }
}
