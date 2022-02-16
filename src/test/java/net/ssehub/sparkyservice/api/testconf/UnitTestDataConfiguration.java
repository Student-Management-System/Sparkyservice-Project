package net.ssehub.sparkyservice.api.testconf;

import java.time.LocalDate;
import java.util.Base64;

import javax.annotation.Nonnull;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.SpringConfig;
import net.ssehub.sparkyservice.api.user.Identity;
import net.ssehub.sparkyservice.api.user.UserRealm;
import net.ssehub.sparkyservice.api.user.dto.SettingsDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto;
import net.ssehub.sparkyservice.api.user.dto.UserDto.ChangePasswordDto;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageImpl;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Spring configuration class which provides a set of beans which should be used
 * during (unit) testing. In order to use it, use the following annotation: <br>
 * <code> @ContextConfiguration(classes= {UnitTestDataConfiguration.class}) </code>
 *
 * @author Marcel
 */
@TestConfiguration
public class UnitTestDataConfiguration {

    public static final String NEW_PASSWORD = "testPassword";
    public static final String OLD_PASSWORD = "oldPw123";
    public static final String USER_EMAIL = "info@test";
    public static final String PAYLOAD = "testPayload";
    public static final String NICK_NAME = "user";
    public static final String USER_NAME = new Identity(NICK_NAME, UserRealm.LOCAL).asUsername();
    public static final LocalDate EXP_DATE = LocalDate.now().plusDays(2);

    /**
     * Creates a simple and complete {@link UserDto} object for testing purposes.
     * 
     * @return complete testing dto
     */
    public static @Nonnull UserDto createExampleDto() {
        var editUserDto = new UserDto();
        editUserDto.username = USER_NAME;
        editUserDto.passwordDto = new ChangePasswordDto();
        editUserDto.passwordDto.newPassword = NEW_PASSWORD;
        editUserDto.passwordDto.oldPassword = OLD_PASSWORD;
        editUserDto.settings = new SettingsDto();
        editUserDto.settings.payload = PAYLOAD;
        editUserDto.settings.emailAddress = USER_EMAIL;
        editUserDto.settings.emailReceive = true;
        editUserDto.settings.wantsAi = true;
        editUserDto.expirationDate = EXP_DATE;
        return editUserDto;
    }

    /**
     * .
     * @return UserStorageImpl
     */
    @Bean
    public UserStorageService userStorageService() {
        return new UserStorageImpl();
    } 

    /**
     * .
     * @return Default user transformer from {@link SpringConfig#userTransformer()}
     */
    @Bean
    @Primary
    public UserExtractionService userTransformer() {
        return new SpringConfig().userTransformer();
    }
}
