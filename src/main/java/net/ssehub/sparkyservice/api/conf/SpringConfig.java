package net.ssehub.sparkyservice.api.conf;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.user.storage.ServiceAccStorageService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageImpl;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.user.transformation.HeavyUserTransformerImpl;
import net.ssehub.sparkyservice.api.user.transformation.UserTransformerService;

/**
 * Default configuration class for spring.
 * 
 * @author marcel
 */
@Configuration
public class SpringConfig {

    public static final String LOCKED_JWT_BEAN = "lockedJwtToken";

    @Autowired
    private ServiceAccStorageService extService;

    /**
     * Defines the PasswordEncoder bean.
     * 
     * @return Using BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sets the cors configuration as bean used by springs Tomcat. 
     * 
     * @return Wide open CORS Configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
        return source;
    }

    /**
     * Defines the Validator bean (which is used for DTO validation).
     * 
     * @return Using {@link LocalValidatorFactoryBean}
     */
    @Bean
    public Validator validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Defines the UserTranfsformer Bean.
     * 
     * @return Using {@link HeavyUserTransformerImpl}
     */
    @Bean
    @Primary
    public UserTransformerService userTransformer() {
        return new HeavyUserTransformerImpl();
    }

    /**
     * Defines the ZuulAuthroizationFilter bean.
     * 
     * @return Using {@link ZuulAuthorizationFilter}
     */
    @Bean
    public ZuulAuthorizationFilter zuulAuthorizationFilter() {
        return new ZuulAuthorizationFilter();
    }


    /**
     * The list of locked jwt tokens from service accounts only. 
     * <br> (Bean definitions happen during startup through this, the list of locked JWT token is only 
     * loaded once per application start).
     * 
     * @return Set of locked JWT tokens of service accounts
     */
    @Bean(LOCKED_JWT_BEAN)
    public Set<String> lockedJwtToken() {
        List<User> serviceAccounts = extService.findAllServiceAccounts();
        try {
            return serviceAccounts.stream()
                    .map(a -> a.getProfileConfiguration().getPayload())
                    .collect(Collectors.toSet());
        } catch (InvalidDataAccessResourceUsageException e) {
            throw new RuntimeException("Database is not available at startup (could not load disabled user");
        }
    }

    /**
     * Defines the IUserService bean.
     * 
     * @return Using {@link UserStorageImpl}
     */
    @Bean
    @Primary
    public UserStorageService iUserService() {
        return new UserStorageImpl();
    }
}
