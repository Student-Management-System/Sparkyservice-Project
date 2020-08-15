package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.testconf.SparkyAssertions.assertDtoValuesEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.user.transformation.MissingDataException;
import net.ssehub.sparkyservice.api.validation.ChangePasswordValidationTest;

//checkstyle: stop exception type check
/**
 * Unit tests for {@link AdminUserModificationImpl}.
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ModificationTestConf.class })
public class AdminModificationTests {

    private Constructor<LocalUserDetails> constructor;

    @Autowired
    private AdminUserModificationImpl modificationService;

    /**
     * Access the (default) constructor of the user class in order to create new
     * instances during test cases. This is done to minimize the dependencies to
     * external factory methods.
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     * @throws SecurityException
     */
    public AdminModificationTests() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        constructor = LocalUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    /**
     * Test for {@link AdminUserModificationImpl#changePasswordFromDto(User, ChangePasswordDto)}.
     * <br>
     * Tests if the password is correctly changed inside user object.
     * 
     * @throws MissingDataException should not happen - would be a result of wrong
     *                              setup method:
     *                              {@link #ChangePasswordValidationTest.createExampleDto()}
     */
    @Test
    public void changePasswordFromDtoTest() throws Exception {
        var userDto = ChangePasswordValidationTest.createExampleDto();
        LocalUserDetails user = constructor.newInstance();
        userDto.passwordDto.newPassword = "hallo123";

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        modificationService.changePasswordFromDto(user, userDto.passwordDto);
        assertTrue(encoder.matches("hallo123", user.getPassword()));
    }

    /**
     * Tests what happens when parameters are null.
     * 
     * @throws Exception
     */
    @Test
    public void changePasswordFromNull() throws Exception {
        LocalUserDetails user = constructor.newInstance();
        var userDto = ChangePasswordValidationTest.createExampleDto();
        var passwordDto = userDto.passwordDto; 
        assertAll(
            () -> assertDoesNotThrow(() -> modificationService.changePasswordFromDto(user, null)),
            () -> assertDoesNotThrow(() -> modificationService.changePasswordFromDto(null, passwordDto)),
            () -> assertDoesNotThrow(() -> modificationService.changePasswordFromDto(null, null)),
            
            () -> assertDoesNotThrow(() -> modificationService.changeUserValuesFromDto(user, null)),
            () -> assertDoesNotThrow(() -> modificationService.changeUserValuesFromDto(null, userDto)),
            () -> assertDoesNotThrow(() -> modificationService.changeUserValuesFromDto(null, null))
        );
    }

    /**
     * Tests if all values will modified.
     * 
     * @throws Exception
     */
    @Test
    public void changeUserValuesTest() throws Exception {
        var userDto = ChangePasswordValidationTest.createExampleDto();
        userDto.role = UserRole.DEFAULT;
        LocalUserDetails user = constructor.newInstance();
        modificationService.changeUserValuesFromDto(user, userDto);
        var userSettings = user.getProfileConfiguration();
        var dtoSettings = userDto.settings;
        assertAll(() -> assertEquals(user.getUsername(), userDto.username, "Username was not changed"),
            () -> assertEquals(user.getRealm(), userDto.realm, "Realm was not changed"),
            () -> assertEquals(user.getRole(), userDto.role, "Role was not changed"),
            () -> assertEquals(user.getExpirationDate().get(), userDto.expirationDate, "Exp. Date not changed"),
            () -> assertEquals(user.getFullName(), userDto.fullName, "Fullname not changed"),
            () -> assertEquals(userSettings.getEmail_address(), dtoSettings.email_address, "Email not changed"),
            () -> assertEquals(userSettings.getPayload(), dtoSettings.payload, "Payload not changed")
        );
    }

    /**
     * Tests for
     * {@link AdminUserModificationImpl#changePasswordFromDto(User, ChangePasswordDto)}.
     * The user role is mandatory and shouldn't be set to null.
     * 
     * @throws Exception
     */
    @Test
    public void mandatoryRoleTest() throws Exception {
        var userDto = ChangePasswordValidationTest.createExampleDto();
        userDto.role = null;
        LocalUserDetails user = constructor.newInstance();
        user.setRole(UserRole.DEFAULT);
        modificationService.changeUserValuesFromDto(user, userDto);

        assertEquals(user.getRole(), UserRole.DEFAULT, "Role was changed to null! But it should be mandatory");
    }

    /**
     * Test for {@link AdminUserModificationImpl#userAsDto(User)}.
     * The used modification service has admin permissions and should set all values to the DTO.
     * 
     * @throws Exception
     */
    @Test
    public void userAsDtoTest() throws Exception {
        var userDto = ChangePasswordValidationTest.createExampleDto();
        LocalUserDetails user = constructor.newInstance();
        userDto.role = UserRole.ADMIN;
        modificationService.changeUserValuesFromDto(user, userDto);
        var modifiedDto = modificationService.userAsDto(user);
        assertDtoValuesEquals(modifiedDto, userDto);
    }
}
