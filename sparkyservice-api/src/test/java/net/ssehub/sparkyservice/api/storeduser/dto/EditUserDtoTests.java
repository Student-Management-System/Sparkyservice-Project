package net.ssehub.sparkyservice.api.storeduser.dto;

import static net.ssehub.sparkyservice.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import net.ssehub.sparkyservice.api.storeduser.StoredUserDetails;
import net.ssehub.sparkyservice.api.storeduser.UserRole;
import net.ssehub.sparkyservice.api.storeduser.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.storeduser.exceptions.MissingDataException;

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
     * Creates a simple and complete {@link UserDto} object for testing purposes. 
     * 
     * @return complete testing dto
     */
    private @Nonnull UserDto createExampleDto() {
        var editUserDto = new UserDto();
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
     * Test for {@link UserDto#defaultApplyPasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is correctly changed inside user object.
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changePasswordDtoTest() {
        user.encodeAndSetPassword(oldPassword);
        var userDto = createExampleDto();
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        UserDto.defaultApplyNewPasswordFromDto(user, userDto.passwordDto);
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultApplyPasswordFromDto(StoredUserDetails, ChangePasswordDto)}. <br>
     * Tests if the password is unchanged if the passwordDto provides the wrong old password. 
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void changeWrongPasswordDtoTest() {
        var passwordDto = new ChangePasswordDto();
        passwordDto.oldPassword = "abcdef";
        passwordDto.newPassword = newPassword;
        user.encodeAndSetPassword(oldPassword);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        
        UserDto.defaultApplyNewPasswordFromDto(user, passwordDto);
        assertFalse(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     *
     * @throws MissingDataException
     */
    @Test
    public void editPasswordFromDtoTest() {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        user.encodeAndSetPassword(oldPassword);
        UserDto.defaultUserDtoEdit(user, createExampleDto());
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * The given User is in the local realm and has no password entity. In reality this should never happen, but when
     * it happen a {@link RuntimeException} should be thrown instead of a NullPointer to indicate that something 
     * went wrong.
     */
    @Test
    public void editPasswordFromDtoNegativeTest() {
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        assertThrows(RuntimeException.class, () -> UserDto.defaultUserDtoEdit(user, createExampleDto()));
    }

    /**
     * Test for {@link UserDto#adminUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * Test if an administrator can edit a password without providing the old password. 
     *      
     * @throws MissingDataException
     */
    @Test
    public void adminEditDtoPasswordTest() {
        var dto = createExampleDto();
        user.encodeAndSetPassword(oldPassword);
        user.setRealm(StoredUserDetails.DEFAULT_REALM);
        dto.passwordDto.oldPassword = null;
        UserDto.adminUserDtoEdit(user, dto);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(newPassword, user.getPassword()), "Password was not changed even though "
                + "the admin mode is on");
    }

    /**
     * The application should proceed even if null values are provided.
     */
    @Test
    public void editNullPasswordTest() {
        assertAll(
                () -> { assertDoesNotThrow(() -> UserDto.defaultApplyNewPasswordFromDto(user, null)); },
                () -> { assertDoesNotThrow(() -> UserDto.adminApplyNewPasswordFromDto(user, null)); },
                () -> { assertDoesNotThrow(() -> UserDto.defaultApplyNewPasswordFromDto(null, null)); },
                () -> { assertDoesNotThrow(() -> UserDto.adminApplyNewPasswordFromDto(null, null)); }
            );
    }

    @ParameterizedTest
    @ValueSource(strings = { " ", "", "null"})
    public void editBlankPasswordNegativeTest(String newPassword) {
        user.encodeAndSetPassword(oldPassword);
        UserDto.adminApplyNewPasswordFromDto(user, "null".equals(newPassword) ? null : newPassword);
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertFalse(encoder.matches(newPassword, user.getPassword()), "Blank passwords shouldn't be changed");
    }
    
    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editEmailFromDtoTest() {
        UserDto.defaultUserDtoEdit(user, createExampleDto());
        assertEquals(userEmaiL, user.getProfileConfiguration().getEmail_address(), "User email was not changed in user "
                + "object");
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * 
     * @throws MissingDataException should not happen - would be a result of wrong setup method: 
     * {@link #createExampleDto()}
     */
    @Test
    public void editNameFromDtoTest() {
        UserDto.defaultUserDtoEdit(user, createExampleDto());
        assertEquals("user", user.getUsername(), "Username was not changed in user "
                + "object");
    }

    @Test
    public void adminEditNameTest() throws MissingDataException {
        UserDto.adminUserDtoEdit(user, createExampleDto());
        assertEquals("user", user.getUsername(), "Username was not changed in user "
                + "object");
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * Tests if the method does not "crash" when values of the dto are <code>null</code>.
     */
    @Test
    public void editNullTest()  {
        var dto = createExampleDto();
        dto.username = null;
        assertDoesNotThrow(() ->  UserDto.defaultUserDtoEdit(user, dto));
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
     * Test for {@link UserDto#username} validation. Tries different input which the validator should not pass.
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
     * Test for {@link UserDto.ChangePasswordDto#newPassword} validation. 
     * Tries different input which the validator should not pass.
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
   
    /**
     * Test for {@link UserDto#adminUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * Tests if an administrator can change the role of an user.
     * 
     * @throws MissingDataException
     */
    @Test
    public void adminEditRoleTest() {
        var dto = createExampleDto();
        dto.role = UserRole.ADMIN;
        UserDto.adminUserDtoEdit(user, dto);
        assertEquals(UserRole.ADMIN.name(), user.getRole(), "An administrator could not change the users role");
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}. 
     * Tests if a user could not change any role. 
     * 
     * @throws MissingDataException
     */
    @Test
    public void defaultEditRoleDeniedTest() {
        var dto = createExampleDto();
        dto.role = UserRole.ADMIN;
        UserDto.defaultUserDtoEdit(user, dto);
        assertNotEquals(UserRole.ADMIN.name(), user.getRole(), "A normal user could change a role of a user");
    }
}
