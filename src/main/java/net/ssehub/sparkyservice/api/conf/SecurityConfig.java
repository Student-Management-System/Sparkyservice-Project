package net.ssehub.sparkyservice.api.conf;


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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;

import net.ssehub.sparkyservice.api.auth.JwtAuthenticationFilter;
import net.ssehub.sparkyservice.api.auth.JwtAuthorizationFilter;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.UserServiceImpl;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Value("${ldap.urls}")
    private String ldapUrls;
    
    @Value("${ldap.base.dn}")
    private String ldapBaseDn;
    
    @Value("${ldap.username}")
    private String ldapSecurityPrincipal;
    
    @Value("${ldap.password}")
    private String ldapPrincipalPassword;
    
    @Value("${ldap.user.dn.pattern}")
    private String ldapUserDnPattern;
    
    @Value("${ldap.enabled}")
    private boolean ldapEnabled;

    @Value("${ldap.domain}")
    private String ldapFullDomain;

    @Value("${ldap.ad}")
    private boolean ldapAd;

    @Value("${recovery.enabled}")
    private String inMemoryEnabled;
    
    @Value("${recovery.password}")
    private String inMemoryPassword;
    
    @Value("${recovery.user}")
    private String inMemoryUser;
    
    @Autowired
    private ConfigurationValues confValues;
    
    @Autowired
    private UserServiceImpl storedUserDetailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
            .csrf().disable()
            .authorizeRequests()
            /*
             * Any other path except own api allowed. They are probably supervised by Zuul.
             */
            .antMatchers("/**").permitAll()
            /*
             * Secure own application: 
             */
            .antMatchers(ControllerPath.SWAGGER).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_AUTH).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_VERIFY).permitAll()
            .antMatchers(ControllerPath.AUTHENTICATION_CHECK).authenticated()
            .antMatchers(ControllerPath.GLOBAL_PREFIX).authenticated() // You must be authenticated by default
            .and()
                .addFilter(new JwtAuthenticationFilter(authenticationManager(), confValues, storedUserDetailService))
                .addFilter(new JwtAuthorizationFilter(authenticationManager(), confValues))
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    }
    
    @Override // allow swagger 
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/v3/api-docs/**", "/swagger-ui/**");
    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        if (inMemoryEnabled != null && Boolean.parseBoolean(inMemoryEnabled)) {
            auth.inMemoryAuthentication()
            .withUser(inMemoryUser)
            .password(passwordEncoder.encode(inMemoryPassword))
            .roles(UserRole.ADMIN.name());
        }
        auth.userDetailsService(storedUserDetailService);
        if (ldapEnabled) {
            if (ldapAd) {
                ActiveDirectoryLdapAuthenticationProvider adProvider = 
                        new ActiveDirectoryLdapAuthenticationProvider(ldapFullDomain, ldapUrls);
                adProvider.setConvertSubErrorCodesToExceptions(true);
                adProvider.setUseAuthenticationRequestCredentials(true);
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
                .userDnPatterns(ldapUserDnPattern);
            }
        }
    }
    
    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
    }
}