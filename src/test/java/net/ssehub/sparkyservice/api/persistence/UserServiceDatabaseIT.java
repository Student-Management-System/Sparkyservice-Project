package net.ssehub.sparkyservice.api.persistence;

import static net.ssehub.sparkyservice.api.testconf.TestSetupMethods.NEW_PASSWORD;
import static net.ssehub.sparkyservice.api.testconf.TestSetupMethods.NICK_NAME;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.ssehub.sparkyservice.api.auth.identity.SparkyUser;
import net.ssehub.sparkyservice.api.auth.identity.UserRealm;
import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTestStorageBeanConf;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.persistence.jpa.user.Password;
import net.ssehub.sparkyservice.api.testconf.RealmBeanConfiguration;
import net.ssehub.sparkyservice.api.useraccess.UserRole;
import net.ssehub.sparkyservice.api.useraccess.UserService;
import net.ssehub.sparkyservice.api.useraccess.dto.UserDto;

/**
 * Provides integrations tests for database modifications for {@link UserService}. #
 * 
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@ContextConfiguration(classes = { JwtTestStorageBeanConf.class, UserServiceDatabaseIT.BeanConf.class, 
    RealmBeanConfiguration.class})
public class UserServiceDatabaseIT {

    /**
     * Provides Bean definition needed by this test class.
     * 
     * @author marcel
     */
    @TestConfiguration
    static class BeanConf {

        @Bean
        public UserService service() {
            return new UserService();
        }
    }

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private JwtAuthReader authReader;

    @Autowired
    private UserService userService;

    @Autowired
    private UserStorageService storageService;

    @Autowired
    @Qualifier("defaultRealm")
    private UserRealm realm;

    @Test
    @DisplayName("Change users fullname edit test")
    public void editFullnameTest() throws JwtTokenReadException {
        SparkyUser user = realm.userFactory().create(NICK_NAME, new Password(NEW_PASSWORD),
            UserRole.ADMIN, true);
        storageService.commit(user);
        UserDto editDto = user.ownDto();
        Authentication authContext = authReader.readToAuthentication(jwtService.createFor(user));

        String newFullname = "SOMETHING ELSE THAN BEFORE";
        editDto.fullName = newFullname;
        UserDto newDto = userService.modifyUser(editDto, authContext);
        assertEquals(newFullname, newDto.fullName, "Fullname was not edited");
    }

    /**
     * When a new user is added twice, it should throw an exception.
     */
    @Test
    public void addUserTest() {
        userService.createInDatabase("test");
        assertThrows(DuplicateEntryException.class, () -> userService.createLocalUser("test"));
    }

    /**
     * Test if a new user is added with correct values.
     */
    @Test
    public void addUserValuesTest() {
        SparkyUser newUser = userService.createInDatabase("name");
        assertAll(
            () -> assertEquals(newUser.getIdentity().realm(), realm, "SparkyUser is in the wrong realm"),
            () -> assertEquals(newUser.getIdentity().nickname(), "name", "SparkyUser with wrong name was created"));
    }
}
