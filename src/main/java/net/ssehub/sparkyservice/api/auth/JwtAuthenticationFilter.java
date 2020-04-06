package net.ssehub.sparkyservice.api.auth;

import java.util.Arrays;
import java.util.List;

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
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.IUserService;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final ConfigurationValues confValues;
    private final IUserService userService;

    private final Logger log = LoggerFactory.getLogger(JwtAuth.class);

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, ConfigurationValues jwtConf,
                                   IUserService userSerivce) {
        this.userService = userSerivce;
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl(ConfigurationValues.AUTH_LOGIN_URL);
        this.confValues = jwtConf;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var userDetails = JwtAuth.extractCredentialsFromHttpRequest(request);
        return authenticationManager.authenticate(userDetails);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) {
        log.info("Successful authentication with JWT");
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            var details = (UserDetails) principal;
            User user;
            try {
                user = userService.getDefaultTransformer().extendFromUserDetails(details);
                if (!userService.isUserInDatabase(user)) {
                    userService.storeUser(user);
                }
                List<UserRole> authorityList = Arrays.asList(user.getRole());
                String token = JwtAuth.createJwtTokenWithRealm(user.getUserName(), authorityList, confValues, 
                        user.getRealm());
                response.addHeader(confValues.getJwtTokenHeader(), confValues.getJwtTokenPrefix() + " " + token);
            } catch (UserNotFoundException e) {
                log.info("A user which is currently logged in, is not found in the database");
            }
        }
    }
}