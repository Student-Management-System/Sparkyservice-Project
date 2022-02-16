package net.ssehub.sparkyservice.api.auth;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.jwt.JwtToken;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
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
    @Value("${ldap.urls:}")
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

    @Value("${ldap.domain:}")
    private String ldapFullDomain;

    @Value("${ldap.ad:false}")
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
    private JwtTokenService jwtService;

    @Autowired 
    private JwtStorageService jwtStorageService;

    @Autowired
    private MemoryDetailsService memoryDetailsService;
    
    @Autowired
    private JwtAuthReader jwtReader;
    
    @Autowired
    private LocalDbDetailsMapper localDetailsMapper;
    
    /**
     * JwtAuthenticationFilter requires ObjectMapper of REST framework, but cannot obtain it via autowiring as it is no
     * component. For this reason we obtain it here and pass it to the constrcutor.
     * Solution based on: https://stackoverflow.com/a/30725492
     */
    @Autowired
    private ObjectMapper jacksonObjectMapper;

    @Override
    protected void configure(HttpSecurity http) throws Exception {       
        var authenticationFilter = new AuthenticationFilter(authenticationManager(), jwtReader, jwtService, 
                jacksonObjectMapper);
        var authorizationFilter = new AuthorizationFilter(authenticationManager(), jwtReader);
        http.cors().and()
            .csrf().disable()
            .authorizeRequests()
            /*
             * Any other path except own api are allowed by other micro services. 
             * They are probably supervised by Zuul.
             */
            //.antMatchers("/**").hasAnyRole(UserRole.SERVICE.name(), UserRole.ADMIN.name())
            .antMatchers(ControllerPath.SWAGGER).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_AUTH).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_VERIFY).permitAll()
            .antMatchers(ControllerPath.HEARTBEAT).permitAll()            
            .antMatchers(ControllerPath.AUTHENTICATION_CHECK).authenticated()
            .and()
                .addFilter(authenticationFilter)
                .addFilter(authorizationFilter)
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
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
        configureInMemory(auth);
        auth.userDetailsService(localDetailsMapper);
        configureLdap(auth);
    }

    /**
     * Configures in memory authentication service.
     * 
     * @param auth - Current builder which is configure with inMemory authentication
     */
    public void configureInMemory(AuthenticationManagerBuilder auth) throws Exception {
        if (inMemoryEnabled) {
            if (inMemoryPassword.isEmpty()) {
                throw new Exception("Set recovery.password or disable the account");
            }
            auth.userDetailsService(memoryDetailsService);
        }
    }

    /**
     * Configures LDAP as login provider.
     * 
     * @param auth
     * @throws Exception 
     */
    public void configureLdap(AuthenticationManagerBuilder auth) throws Exception {
        if (ldapEnabled) {
            if (ldapAd) {
                ActiveDirectoryLdapAuthenticationProvider adProvider = 
                        new ActiveDirectoryLdapAuthenticationProvider(ldapFullDomain, ldapUrls);
                adProvider.setConvertSubErrorCodesToExceptions(true);
                adProvider.setUseAuthenticationRequestCredentials(true);
                adProvider.setUserDetailsContextMapper(ldapContextMapper);
                if (ldapUserDnPattern != null && ldapUserDnPattern.trim().length() > 0) {
                    adProvider.setSearchFilter(ldapUserDnPattern);
                }
                auth.authenticationProvider(adProvider);
                auth.eraseCredentials(false);
            } else {
                auth.ldapAuthentication()
                    .contextSource()
                    .url(ldapUrls + ldapBaseDn)
                    .managerDn(ldapSecurityPrincipal)
                    .managerPassword(ldapPrincipalPassword)
                    .and()
                    .userDnPatterns(ldapUserDnPattern)
                    .userDetailsContextMapper(ldapContextMapper);
            }
        }
    }

    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}