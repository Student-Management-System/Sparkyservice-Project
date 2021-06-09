package net.ssehub.sparkyservice.api.integration.user;

import static net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration.NEW_PASSWORD;
import static net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration.USER_NAME;
import static net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration.createExampleDto;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.jpa.user.Password;
import net.ssehub.sparkyservice.api.testconf.JwtTestBeanConf;
import net.ssehub.sparkyservice.api.testconf.UnitTestDataConfiguration;
import net.ssehub.sparkyservice.api.user.LocalUserFactory;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.UserRole;
import net.ssehub.sparkyservice.api.user.UserService;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Provides integrations tests for database modifications for {@link UserService}.
 * #
 * @author marcel
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@ContextConfiguration(
    classes = { UnitTestDataConfiguration.class, JwtTestBeanConf.class, UserServiceDatabaseIT.BeanConf.class }
)
@EnableJpaRepositories("net.ssehub.sparkyservice.api")
public class UserServiceDatabaseIT {

    /**
     * Provides Bean definition needed by this test class.
     * 
     * @author marcel
     */
    @TestConfiguration
    static class BeanConf {

        /**
         * Bean def. for user service
         * 
         * @return UserService
         */
        @Bean
        public UserService service() {
            return new UserService();
        }
    }

    @Autowired
    private JwtTokenService jwtService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserStorageService storageService;

    /**
     * @throws JwtTokenReadException
     */
    @Test
    @DisplayName("Change users fullname edit test")
    public void editFullnameTest() throws JwtTokenReadException {
        var editDto = createExampleDto();
        editDto.role = UserRole.ADMIN;
        editDto.realm = UserRealm.LOCAL;
        SparkyUser user = new LocalUserFactory().create(USER_NAME, new Password(NEW_PASSWORD), editDto.role, true);
        storageService.commit(user);
        Authentication authContext = jwtService.readToAuthentication(jwtService.createFor(user));

        String newFullname = "SOMETHING ELSE THAN BEFORE";
        editDto.fullName = newFullname;
        UserDto newDto = userService.modifyUser(editDto, authContext);
        assertEquals(newFullname, newDto.fullName, "Fullname was not edited");
    }
}
