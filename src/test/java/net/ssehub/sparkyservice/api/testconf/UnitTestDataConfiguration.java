package net.ssehub.sparkyservice.api.testconf;

import java.util.Base64;

import javax.annotation.Nonnull;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import net.ssehub.sparkyservice.api.auth.LocalLoginDetailsMapper;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.SpringConfig;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.ServiceAccStorageService;
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

    /**
     * @return ServiceAccStorageService TODO delete me :)
     */
    @Bean
    public ServiceAccStorageService service() {
        return new ServiceAccStorageService();
    }

    /**
     * The login information mapper for local storage logins.
     * @return LocalLoginDetailsMapper
     */
    @Bean
    public LocalLoginDetailsMapper mapper() {
        return new LocalLoginDetailsMapper();
    }

    @Nonnull
    public static JwtSettings sampleJwtConf() {
        JwtSettings jwtConf = new JwtSettings();
        var secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String secretString = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        jwtConf.setSecret(secretString);
        jwtConf.setAudience("Y");
        jwtConf.setHeader("Authorization");
        jwtConf.setIssuer("TEST");
        jwtConf.setType("Bearer");
        return jwtConf;
    }
}
