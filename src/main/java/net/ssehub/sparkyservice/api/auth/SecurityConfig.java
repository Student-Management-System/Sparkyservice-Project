package net.ssehub.sparkyservice.api.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthConverter;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.config.ControllerPath;

/**
 * Springs security configuration loaded at startup.
 * 
 * @author marcel
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
//checkstyle: stop exception type check
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtStorageService jwtStorageService;

    @Autowired
    private JwtAuthConverter authConverter;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        var authorizationFilter = new SetAuthenticationFilter(authConverter);
        http.cors().and().csrf().disable().authorizeRequests().antMatchers(ControllerPath.SWAGGER).permitAll().and()
                .addFilterBefore(authorizationFilter, AnonymousAuthenticationFilter.class).sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        fillJwtCache();
    }

    /**
     * Fills the {@link JwtCache} with initial values from the database. It stores
     * all JWT token.
     */
    private void fillJwtCache() {
        final List<JwtToken> lockedJwtToken = jwtStorageService.findAll();
        if (lockedJwtToken != null) {
            JwtCache.initNewCache(lockedJwtToken, jwtStorageService);
        }
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/v3/api-docs/**", "/swagger-ui/**");
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {

                /*
                 * For LDAP Login. Currently this is disabled because we cant provide this as
                 * bean to use it in a the ContextAuthenticationManager
//                 */
//                auth.ldapAuthentication().contextSource().url(ldapUrl + ldapBaseDn).managerDn(ldapSecurityPrincipal)
//                        .managerPassword(ldapPrincipalPassword).and().userDnPatterns(ldapUserDnPattern)
//                        .userDetailsContextMapper(ldapContextMapper);
//                throw new UnsupportedOperationException("Currently only AD is supported for LDAP connections.");

    }

    /**
     * Sets the cors configuration as bean used by springs Tomcat.
     * 
     * @return Wide open CORS Configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        final var source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    
    /**
     * Open API Definition. Sets authentication mechanism, version and the title.
     * 
     * @param appVersion
     * @return Configured OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI(@Value("${springdoc.version}") String appVersion) {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearer-key",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .info(new Info().title("Sparkyservice API").version(appVersion)
                        .license(new License().name("Apache 2.0").url("")));
    }
}