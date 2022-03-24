package net.ssehub.sparkyservice.api.user.modification;

import static net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration.createExampleDto;
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
import net.ssehub.sparkyservice.api.user.LocalUserDetails;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;


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
        var userDto = createExampleDto();
        LocalUserDetails user = constructor.newInstance();
        userDto.passwordDto.newPassword = "hallo123";

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        modificationService.update(user, userDto);
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
        var userDto = createExampleDto();
        assertAll(
            () -> assertDoesNotThrow(() -> modificationService.update(user, null)),
            () -> assertDoesNotThrow(() -> modificationService.update(null, userDto)),
            () -> assertDoesNotThrow(() -> modificationService.update(null, null))
        );
    }

    /**
     * Tests if all values will modified.
     * 
     * @throws Exception
     */
    @Test
    public void changeUserValuesTest() throws Exception {
        var userDto = createExampleDto();
        userDto.role = UserRole.DEFAULT;
        LocalUserDetails user = constructor.newInstance();
        modificationService.update(user, userDto);
        var userSettings = user.getSettings();
        var dtoSettings = userDto.settings;
        
        assertAll(
            () -> assertEquals(user.getUsername(), userDto.username, "Username was not changed"),
            () -> assertEquals(user.getRole(), userDto.role, "Role was not changed"),
            () -> assertEquals(user.getExpireDate().get(), userDto.expirationDate, "Exp. Date not changed"),
            () -> assertEquals(user.getFullname(), userDto.fullName, "Fullname not changed"),
            () -> assertEquals(userSettings.getEmailAddress(), dtoSettings.emailAddress, "Email not changed"),
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
        var userDto = createExampleDto();
        userDto.role = null;
        LocalUserDetails user = constructor.newInstance();
        user.setRole(UserRole.DEFAULT);
        modificationService.update(user, userDto);

        assertEquals(user.getRole(), UserRole.DEFAULT, "Role was changed to null! But it should be mandatory");
    }

    /**
     * Test for {@link AdminUserModificationImpl#asDto(User)}.
     * The used modification service has admin permissions and should set all values to the DTO.
     * 
     * @throws Exception
     */
    @Test
    public void userAsDtoTest() throws Exception {
        var testDto = createExampleDto();
        testDto.role = UserRole.ADMIN;
        var testUserFromDto = UserRealm.LOCAL.getUserFactory().create(testDto);
        var userDto = modificationService.asDto(testUserFromDto);
        assertTrue(userDto.equals(testDto));
    }

    // TODO create test case for default user creation
//    public void defaultRealmTest() throws Exception {
//        var userDto = ChangePasswordValidationTest.createExampleDto();
//        LocalUserDetails user = constructor.newInstance();
//        modificationService.update(user, userDto);
//        var modifiedDto = modificationService.asDto(user);
//        assertDtoValuesEquals(modifiedDto, userDto);
//    } 
}
