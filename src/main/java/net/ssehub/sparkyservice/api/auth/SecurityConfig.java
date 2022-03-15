package net.ssehub.sparkyservice.api.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthConverter;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtCache;
import net.ssehub.sparkyservice.api.auth.jwt.storage.JwtStorageService;
import net.ssehub.sparkyservice.api.auth.ldap.LdapContextMapper;
import net.ssehub.sparkyservice.api.auth.provider.LocalDbDetailsMapper;
import net.ssehub.sparkyservice.api.auth.provider.MemoryDetailsService;
import net.ssehub.sparkyservice.api.conf.ControllerPath;

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
    @Value("${ldap.urls:none}")
    private String ldapUrls;
    
    @Value("${ldap.base.dn:}")
    private String ldapBaseDn;
    
    @Value("${ldap.username:}")
    private String ldapSecurityPrincipal;
    
    @Value("${ldap.password:}")
    private String ldapPrincipalPassword;
    
    @Value("${ldap.user.dn.pattern:}")
    private String ldapUserDnPattern;
    
    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.ad.domain:}")
    private String ldapFullDomain;

    @Value("${ldap.ad.enabled:false}")
    private boolean ldapAd;

    @Value("${recovery.enabled:false}")
    private boolean inMemoryEnabled;
    
    @Value("${recovery.password:}")
    private String inMemoryPassword;
    
    @Value("${recovery.user:user}")
    private String inMemoryUser;
    
    @Autowired
    private LdapContextMapper ldapContextMapper;
    
    @Autowired 
    private JwtStorageService jwtStorageService;

    @Autowired
    private MemoryDetailsService memoryDetailsService;
    
    @Autowired
    private LocalDbDetailsMapper localDetailsMapper;
    
    @Autowired
    private JwtAuthConverter authConverter;
    
    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    @Autowired
    private PasswordEncoder pwEncoder;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // todo use springs auth filter ? 
        var authorizationFilter = new SetAuthenticationFilter(authenticationManager(), authConverter);
        http.cors().and()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(ControllerPath.SWAGGER).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_AUTH).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_VERIFY).permitAll()
            .antMatchers(ControllerPath.HEARTBEAT).permitAll()            
            .antMatchers(ControllerPath.AUTHENTICATION_CHECK).authenticated()
            .and()
                .addFilter(authorizationFilter)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.exceptionHandling().authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
        fillJwtCache();
    }

    /**
     * Fills the {@link JwtCache} with initial values from the database. It stores all JWT token.
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
        if (inMemoryEnabled) {
            if (inMemoryPassword.isEmpty()) {
                throw new UnsupportedOperationException("Set recovery.password or disable the account");
            }
            auth.authenticationProvider(memoryAuthProvider());
        }
        auth.authenticationProvider(localDbAuthProvider());
        if (ldapEnabled) {
            if (ldapAd) {
                auth.authenticationProvider(adLdapAuthProvider()).eraseCredentials(false);;
            } else {
                /*
                 * For LDAP Login. Currently this is disabled because we cant provide this as bean to use it in 
                 * a the ContextAuthenticationManager  
                 */
//                auth.ldapAuthentication()
//                .contextSource()
//                .url(ldapUrls + ldapBaseDn)
//                .managerDn(ldapSecurityPrincipal)
//                .managerPassword(ldapPrincipalPassword)
//                .and()
//                .userDnPatterns(ldapUserDnPattern)
//                .userDetailsContextMapper(ldapContextMapper);
                throw new UnsupportedOperationException("Currently only AD is supported for LDAP connections.");
            }
        }
    }

   @ConditionalOnProperty(value = "recovery.enabled", havingValue = "true")
   @Bean("memoryAuthProvider")
   public AuthenticationProvider memoryAuthProvider() {
        var prov = new DaoAuthenticationProvider();
        prov.setUserDetailsService(memoryDetailsService);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }
    
    @Bean("localDbAuthProvider")
    public AuthenticationProvider localDbAuthProvider() {
        var prov = new DaoAuthenticationProvider();
        prov.setUserDetailsService(localDetailsMapper);
        prov.setPasswordEncoder(pwEncoder);
        return prov;
    }
    
    @Bean("adLdapAuthProvider")
    @ConditionalOnProperty(value = "ldap.ad.enabled", havingValue = "true")
    public AuthenticationProvider adLdapAuthProvider() {
        ActiveDirectoryLdapAuthenticationProvider adProvider = 
                new ActiveDirectoryLdapAuthenticationProvider(ldapFullDomain, ldapUrls);
        adProvider.setConvertSubErrorCodesToExceptions(true);
        adProvider.setUseAuthenticationRequestCredentials(true);
        adProvider.setUserDetailsContextMapper(ldapContextMapper);
        if (ldapUserDnPattern != null && ldapUserDnPattern.trim().length() > 0) {
            adProvider.setSearchFilter(ldapUserDnPattern);
        }
        return adProvider;
    }

    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
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
}