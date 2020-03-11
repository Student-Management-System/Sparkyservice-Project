package net.ssehub.sparkyservice.api.conf;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.ssehub.sparkyservice.api.auth.JwtAuthenticationFilter;
import net.ssehub.sparkyservice.api.auth.JwtAuthorizationFilter;
import net.ssehub.sparkyservice.api.storeduser.StoredUserService;
import net.ssehub.sparkyservice.api.storeduser.UserRole;

@Configuration
@EnableWebSecurity
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
    private String ldapEnabled;

    @Value("${recovery.enabled}")
    private String inMemoryEnabled;
    
    @Value("${recovery.password}")
    private String inMemoryPassword;
    
    @Value("${recovery.user}")
    private String inMemoryUser;
    
    @Autowired
    private ConfigurationValues confValues;
    
    @Autowired
    private StoredUserService storedUserDetailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private String addUserPath = ControllerPath.GLOBAL_PREFIX
            + ControllerPath.MANAGEMENT_PREFIX
            + ControllerPath.MANAGEMENT_ADD_USER;
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and()
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(ControllerPath.SWAGGER).permitAll()
            //.antMatchers(addUserPath).hasRole(UserRole.ADMIN.name()) // admin: allowed to add users
            //.antMatchers("/**").permitAll()//maybe remove later
            .anyRequest().authenticated()
            .and()
                .addFilter(new JwtAuthenticationFilter(authenticationManager(), confValues))
                .addFilter(new JwtAuthorizationFilter(authenticationManager(), confValues))
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    }
    
    @Override // allow swagger 
    // TODO Marcel: test if necessary
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/v2/api-docs",
                                   "/configuration/ui",
                                   "/swagger-resources/**",
                                   "/configuration/security",
                                   "/swagger-ui.html",
                                   "/webjars/**");
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
        if (Boolean.parseBoolean(ldapEnabled)) {
            // TODO check if database is online
            auth.ldapAuthentication()
            .contextSource()
            .url(ldapUrls + ldapBaseDn)
            .managerDn(ldapSecurityPrincipal)
            .managerPassword(ldapPrincipalPassword)
            .and()
            .userDnPatterns(ldapUserDnPattern);
        }
    }
    
    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
            return super.authenticationManagerBean();
    }
}