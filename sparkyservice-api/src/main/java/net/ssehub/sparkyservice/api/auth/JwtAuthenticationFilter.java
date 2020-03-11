package net.ssehub.sparkyservice.api.auth;


import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final ConfigurationValues confValues;
    
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, ConfigurationValues jwtConf) {
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
        String token = JwtAuthentication.createJwtToken(authentication, confValues);
        response.addHeader(confValues.getJwtTokenHeader(), confValues.getJwtTokenPrefix() + " " + token);
    }
}
