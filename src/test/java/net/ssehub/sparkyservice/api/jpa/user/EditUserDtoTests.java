package net.ssehub.sparkyservice.api.jpa.user;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;

public class EditUserDtoTests {

    @Nonnull
    private LocalUserDetails user;
    private static final String newPassword = "testPassword";
    private static final String oldPassword = "oldPw123";
    private static final String userEmaiL = "info@test";

    /**
     * Access a the (default) {@link LocalUserDetails} via java reflections and creates a new instance. 
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
        Constructor<LocalUserDetails> constructor = LocalUserDetails.class.getDeclaredConstructor();
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
        editUserDto.realm = UserRealm.UNKNOWN;
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
     * Test for {@link UserDto#defaultApplyPasswordFromDto(LocalUserDetails, ChangePasswordDto)}. <br>
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

        User.defaultApplyNewPasswordFromDto(user, userDto.passwordDto);
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultApplyPasswordFromDto(LocalUserDetails, ChangePasswordDto)}. <br>
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
        
        User.defaultApplyNewPasswordFromDto(user, passwordDto);
        assertFalse(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     *
     * @throws MissingDataException
     */
    @Test
    public void editPasswordFromDtoTest() {
        user.setRealm(LocalUserDetails.DEFAULT_REALM);
        user.encodeAndSetPassword(oldPassword);
        User.defaultUserDtoEdit(user, createExampleDto());
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        assertTrue(encoder.matches(newPassword, user.getPassword()));
    }

    /**
     * Test for {@link UserDto#defaultUserDtoEdit(net.ssehub.sparkyservice.db.user.StoredUser, UserDto)}.<br>
     * The given User is in the local realm and has no password entity. In reality this should never happen, but when
     * it happens, the application should not throw anything. 
     * went wrong.
     */
    @Test
    public void editPasswordFromDtoNegativeTest() {
        user.setRealm(LocalUserDetails.DEFAULT_REALM);
        assertDoesNotThrow(() -> User.defaultUserDtoEdit(user, createExampleDto()));
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
        user.setRealm(LocalUserDetails.DEFAULT_REALM);
        dto.passwordDto.oldPassword = null;
        User.adminUserDtoEdit(user, dto);
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
                () -> { assertDoesNotThrow(() -> User.defaultApplyNewPasswordFromDto(user, null)); },
                () -> { assertDoesNotThrow(() -> User.adminApplyNewPasswordFromDto(user, null)); },
                () -> { assertDoesNotThrow(() -> User.defaultApplyNewPasswordFromDto(null, null)); },
                () -> { assertDoesNotThrow(() -> User.adminApplyNewPasswordFromDto(null, null)); }
            );
    }

    @ParameterizedTest
    @ValueSource(strings = { " ", "", "null"})
    public void editBlankPasswordNegativeTest(String newPassword) {
        user.encodeAndSetPassword(oldPassword);
        User.adminApplyNewPasswordFromDto(user, "null".equals(newPassword) ? null : newPassword);
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
        User.defaultUserDtoEdit(user, createExampleDto());
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
        User.defaultUserDtoEdit(user, createExampleDto());
        assertEquals("user", user.getUsername(), "Username was not changed in user "
                + "object");
    }

    @Test
    public void adminEditNameTest() throws MissingDataException {
        User.adminUserDtoEdit(user, createExampleDto());
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
        assertDoesNotThrow(() ->  User.defaultUserDtoEdit(user, dto));
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
        User.adminUserDtoEdit(user, dto);
        assertEquals(UserRole.ADMIN, user.getRole(), "An administrator could not change the users role");
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
        User.defaultUserDtoEdit(user, dto);
        assertNotEquals(UserRole.ADMIN.name(), user.getRole(), "A normal user could change a role of a user");
    }

    @Test
    public void userAsDtoTest() {
        var dto = createExampleDto();
        User.defaultUserDtoEdit(user, dto);
        var userDto = user.asDto();
        assertAll(
                () -> assertEquals(dto.realm, userDto.realm, "Realm not correctly changed or transformed"),
                //() -> assertEquals(dto.role, userDto.role, "Role not correctly changed or transformed"), Currently not implemented 
                () -> assertEquals(dto.username, userDto.username, "Username not correctly changed or transformed"),
                () ->assertEquals(dto.settings.email_address, userDto.settings.email_address, 
                        "Email not correctly changed or transformed")
            );
    }
}
