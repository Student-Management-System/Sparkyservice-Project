package net.ssehub.sparkyservice.api.conf;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

import net.ssehub.sparkyservice.api.auth.JwtAuthenticationFilter;
import net.ssehub.sparkyservice.api.auth.JwtAuthorizationFilter;
import net.ssehub.sparkyservice.api.auth.LocalLoginDetailsMapper;
import net.ssehub.sparkyservice.api.auth.MemoryLoginDetailsService;
import net.ssehub.sparkyservice.api.auth.ldap.SparkyLdapUserDetailsMapper;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.user.transformation.UserTransformerService;

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
    private JwtSettings jwtSettings;
    
    @Autowired
    private LocalLoginDetailsMapper localDetailsMapper;

    @Autowired
    private UserStorageService storageService;
    
    @Autowired
    @Qualifier(SpringConfig.LOCKED_JWT_BEAN)
    private Set<String> lockedJwtToken;

    @Autowired
    private UserTransformerService transformator;

    @Autowired
    private MemoryLoginDetailsService memoryDetailsService;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
            .csrf().disable()
            .authorizeRequests()
            /*
             * Any other path except own api are allowed by other micro services. 
             * They are probably supervised by Zuul.
             */
            //.antMatchers("/**").hasAnyRole(UserRole.SERVICE.name(), UserRole.ADMIN.name())
            /*
             * Secure own application: 
             */
            .antMatchers(ControllerPath.SWAGGER).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_AUTH).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_VERIFY).permitAll()
            .antMatchers(ControllerPath.HEARTBEAT).permitAll()            
            .antMatchers(ControllerPath.AUTHENTICATION_CHECK).authenticated()
            .antMatchers(ControllerPath.GLOBAL_PREFIX).authenticated() // You must be authenticated by default
            .and()
                .addFilter(
                    new JwtAuthenticationFilter(authenticationManager(), jwtSettings, storageService, transformator)
                )
                .addFilter(
                    new JwtAuthorizationFilter(authenticationManager(), jwtSettings, lockedJwtToken, transformator)
                )
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    }
    
    @Override // allow swagger 
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
//            auth.inMemoryAuthentication().set
//                .withUser(inMemoryUser)
//                .password(passwordEncoder.encode(inMemoryPassword))
//                .roles(UserRole.ADMIN.name())
//                .and()
//                .withUser(memUser));
//                .password(passwordEncoder.encode(inMemoryPassword));
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
            var ldapMapper = new SparkyLdapUserDetailsMapper(storageService);
            if (ldapAd) {
                ActiveDirectoryLdapAuthenticationProvider adProvider = 
                        new ActiveDirectoryLdapAuthenticationProvider(ldapFullDomain, ldapUrls);
                adProvider.setConvertSubErrorCodesToExceptions(true);
                adProvider.setUseAuthenticationRequestCredentials(true);
                adProvider.setUserDetailsContextMapper(ldapMapper);
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
                    .userDetailsContextMapper(ldapMapper);
            }
        }
    }

    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}