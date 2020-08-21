package net.ssehub.sparkyservice.api.testconf;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import net.ssehub.sparkyservice.api.auth.LocalLoginDetailsMapper;
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

    @Bean
    public UserStorageService userStorageService() {
        return new UserStorageImpl();
    } 

    @Bean
    @Primary
    public UserExtractionService userTransformer() {
        return new SpringConfig().userTransformer();
    }

    @Bean
    public ServiceAccStorageService service() {
        return new ServiceAccStorageService();
    }

    @Bean
    public LocalLoginDetailsMapper mapper() {
        return new LocalLoginDetailsMapper();
    }
}
