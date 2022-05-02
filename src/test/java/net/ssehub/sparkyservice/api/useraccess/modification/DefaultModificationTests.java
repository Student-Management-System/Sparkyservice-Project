package net.ssehub.sparkyservice.api.useraccess.modification;

import static net.ssehub.sparkyservice.api.testconf.TestSetupMethods.createExampleDto;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.local.LocalRealm;
import net.ssehub.sparkyservice.api.auth.local.LocalUserDetails;
import net.ssehub.sparkyservice.api.useraccess.UserRole;

//checkstyle: stop exception type check
/**
* Unit tests for {@link DefaultModificationTests}.
* 
* @author marcel
*/
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ModificationTestConf.class })
public class DefaultModificationTests {

    private Constructor<LocalUserDetails> constructor;

    @Autowired
    private DefaultUserModificationImpl modificationService;

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
    public DefaultModificationTests() 
        throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        constructor = LocalUserDetails.class.getDeclaredConstructor();
        constructor.setAccessible(true);
    }

    /**
     * Tests if the DTO is correctly created without admin specific fields.
     * 
     * @throws Exception
     */
    @Test
    public void userAsDtoTest() throws Exception {
        LocalUserDetails user = LocalUserDetails.newLocalUser("test", new LocalRealm(), "test", UserRole.DEFAULT);
        user.getSettings().setEmailAddress("test@test");
        user.getSettings().setPayload("test");
        user.setExpireDate(LocalDate.now());
        
        var modifiedDto = modificationService.asDto(user);
        var dtoSettings = modifiedDto.settings;
        assertAll(
            () -> assertEquals(null, modifiedDto.expirationDate, "Exp Date is available!"),
            () -> assertEquals(user.getFullname(), modifiedDto.fullName, "Fullname not avaiable"),
            () -> assertEquals(null, dtoSettings.payload, "Payload is available"),
            () -> assertEquals("test@test", dtoSettings.emailAddress, "Email not in dto")
        );
    }

    @Test
    public void changePasswordPositiveTest() throws Exception {
        var userDto = createExampleDto();
        LocalUserDetails user = constructor.newInstance();
        user.encodeAndSetPassword("oldPw");
        userDto.passwordDto.newPassword = "hallo123";
        userDto.passwordDto.oldPassword = "oldPw";

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        modificationService.update(user, userDto);
        assertTrue(encoder.matches("hallo123", user.getPassword()), "Password not changed");
    }

    @Test
    public void changePasswordConditionTest() throws Exception {
        var userDto = createExampleDto();
        LocalUserDetails user = constructor.newInstance();
        user.encodeAndSetPassword("oldPw");
        userDto.passwordDto.newPassword = "hallo123";
        userDto.passwordDto.oldPassword = "wrongOldPw";

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        modificationService.update(user, userDto);
        assertFalse(encoder.matches("hallo123", user.getPassword()), "Password was changed even when old password"
                + " not matched");
    }
}
