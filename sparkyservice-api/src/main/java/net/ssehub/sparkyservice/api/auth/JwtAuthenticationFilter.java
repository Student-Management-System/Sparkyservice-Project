package net.ssehub.sparkyservice.api.auth;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.storeduser.IStoredUserService;
import net.ssehub.sparkyservice.api.storeduser.StoredUserService;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final ConfigurationValues confValues;
    private final StoredUserService userService;

    private final Logger log = LoggerFactory.getLogger(JwtAuthentication.class);
    
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, 
                                   ConfigurationValues jwtConf, IStoredUserService userSerivce) {
        this.userService = (StoredUserService) userSerivce;
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl(ConfigurationValues.AUTH_LOGIN_URL);
        this.confValues = jwtConf;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var userDetails = JwtAuthentication.extractCredentialsFromHttpRequest(request);
        return authenticationManager.authenticate(userDetails);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) {
        log.info("Successful authentication with JWT");
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            try {
                var user = userService.convertUserDetailsToStoredUser((UserDetails) principal);
                userService.storeUser(user);
            } catch (UnsupportedOperationException e) {
                log.warn("Could not store authenticated user");
            }
        }
        String token = JwtAuthentication.createJwtToken(authentication, confValues);
        response.addHeader(confValues.getJwtTokenHeader(), confValues.getJwtTokenPrefix() + " " + token);
    }
}
