package net.ssehub.sparkyservice.api.conf;

import javax.validation.Validator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import net.ssehub.sparkyservice.api.routing.ZuulAuthorizationFilter;
import net.ssehub.sparkyservice.api.user.extraction.SimpleExtractionImpl;
import net.ssehub.sparkyservice.api.user.extraction.UserExtractionService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageImpl;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;

/**
 * Default configuration class for spring.
 * 
 * @author marcel
 */
@Configuration
public class SpringConfig {

    public static final String LOCKED_JWT_BEAN = "lockedJwtToken";

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
     * @return Using {@link SimpleExtractionImpl}
     */
    @Bean
    @Primary
    public UserExtractionService userTransformer() {
        return new SimpleExtractionImpl();
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
