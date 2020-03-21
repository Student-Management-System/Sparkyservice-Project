package net.ssehub.sparkyservice.api.storeduser.dto;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.Nonnull;
import javax.validation.Validation;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.storeduser.MissingDataException;
import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto;
import net.ssehub.sparkyservice.api.storeduser.dto.SettingsDto;
import net.ssehub.sparkyservice.api.storeduser.dto.EditUserDto.ChangePasswordDto;

public class EditUserDtoTests {

    @Nonnull
    private StoredUserDetails user;
    private static final String newPassword = "testPassword";
    private static final String oldPassword = "oldPw123";
    private static final String userEmaiL = "info@test";

    /**
     * Access a the (default) {@link StoredUserDetails} via java reflections and creates a new instance. 
     * This is done to minimize the dependencies to external factory methods.
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public EditUserDtoTests() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Constructor<StoredUserDetails> constructor = StoredUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        user = notNull(constructor.newInstance());
    }

    /**
     * Creates a simple and complete {@link EditUserDto} object for testing purposes. 
     * 
     * @return complete testing dto
     */
    private @Nonnull EditUserDto createExampleDto() {
        var editUserDto = new EditUserDto();
        editUserDto.username = "user";
        editUserDto.realm = "realm";
        editUserDto.passwordDto = new ChangePasswordDto();
        editUserDto.passwordDto.newPassword = newPassword;
        editUserDto.passwordDto.oldPassword = oldPassword;
        editUserDto.settings = new SettingsDto();
        editUserDto.settings.email_address = userEmaiL;
        editUserDto.settings.email_receive = true;
        editUserDto.settings.wantsAi = true;
        return editUserDto;
    }

    /**
     * Test for {@link EditUserDto#changePasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is correctly changed inside user object.
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changePasswordDtoTest() throws MissingDataException {
        user.encodeAndSetPassword(oldPassword);
        var userDto = createExampleDto();
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        EditUserDto.changePasswordFromDto(user, userDto.passwordDto);
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link EditUserDto#changePasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is unchanged if the passwordDto provides the wrong old password. 
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changeWrongPasswordDtoTest() throws MissingDataException {
        var passwordDto = new ChangePasswordDto();
        passwordDto.oldPassword = "abcdef";
        passwordDto.newPassword = newPassword;
        user.encodeAndSetPassword(oldPassword);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        EditUserDto.changePasswordFromDto(user, passwordDto);
        assertFalse(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     *
     * @throws MissingDataException
     */
    @Test
    public void editUserPasswordFromDtoTest() throws MissingDataException {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        user.encodeAndSetPassword(oldPassword);
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * The given User is in the local realm and has no password entitiy. In reality this should never happen, but when
     * it happen a {@link RuntimeException} should be thrown to indicate that something went wrong.
     */
    @Test
    public void editUserPasswordFromDtoNegativeTest() {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        assertThrows(RuntimeException.class, () -> EditUserDto.editUserFromDtoValues(user, createExampleDto()));
    }

    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editUserEmailFromDtoTest() throws MissingDataException {
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        assertEquals(userEmaiL, user.getProfileConfiguration().getEmail_address(), "User email was not changed in user "
                + "object");
    }

    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editUserNameFromDtoTest() throws MissingDataException {
        EditUserDto.editUserFromDtoValues(user, createExampleDto());
        assertEquals("user", user.getUsername(), "Username was not changed in user "
                + "object");
    }

    /**
     * Test for {@link EditUserDto#editUserFromDtoValues(net.ssehub.sparkyservice.db.user.StoredUser, EditUserDto)}.<br>
     * Tests if the method throws the correct exception when values of the dto are <code>null</code>.
     */
    @Test
    public void editNullTest()  {
        var dto = createExampleDto();
        dto.username = null;
        assertThrows(MissingDataException.class, () ->  EditUserDto.editUserFromDtoValues(user, dto));
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
     * Test for {@link EditUserDto#username} validation. Tries different input which the validator should not pass.
     */
    @ParameterizedTest
    @ValueSource(strings = { " ", "", "null" })
    public void editDtoValidationUsernameTest(String username) {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var userDto = createExampleDto();
        assumeTrue(validator.validate(userDto).isEmpty(), "The provided example dto is not correct. Skip this test");
        if ("null".equals(username)) {
            username = null;
        }
        userDto.username = username;
        assertFalse(validator.validate(userDto).isEmpty(), "The validator pass invalid values for username.");
    }
}
