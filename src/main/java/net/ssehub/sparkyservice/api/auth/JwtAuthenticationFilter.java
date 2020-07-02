package net.ssehub.sparkyservice.api.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.jpa.user.User;
import net.ssehub.sparkyservice.api.jpa.user.UserRole;
import net.ssehub.sparkyservice.api.user.IUserService;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.NullHelpers;

/**
 * A Filter which handles all authentication requests and actually handles the login.
 * @author marcel
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtSettings jwtConf;
    private final IUserService userService;
    private final Logger log = LoggerFactory.getLogger(JwtAuth.class);

    /**
     * Constructor for the general Authentication filter. In most cases filters are set in the spring security 
     * configuration. 
     * 
     * @param authenticationManager
     * @param jwtConf
     * @param userSerivce
     */
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf,
                                   IUserService userSerivce) {
        this.userService = userSerivce;
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl(ConfigurationValues.AUTH_LOGIN_URL);
        this.jwtConf = jwtConf;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var userDetails = JwtAuth.extractCredentialsFromHttpRequest(request);
        return authenticationManager.authenticate(userDetails);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) {
        log.info("Successful authentication with JWT");
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            var details = (UserDetails) principal;
            try {
                User user = userService.getDefaultTransformer().extendFromUserDetails(details);
                if (!userService.isUserInDatabase(user)) {
                    userService.storeUser(user);
                }
                AuthenticationInfoDto authDto = buildAuthenticatioInfoFromUser(user);
                setResponseValue(NullHelpers.notNull(response), authDto);
            } catch (UserNotFoundException e) {
                log.info("A user which is currently logged in, is not found in the database");
            }
        }
    }

    /**
     * Builds an authentication DTO with the information provided by user object. 
     * 
     * @param user - object which contains all necessary information.
     * @return AuthenticationDTO currently without {@link TokenDto#expiration}
     */
    private @Nonnull AuthenticationInfoDto buildAuthenticatioInfoFromUser(@Nonnull User user) {
        List<UserRole> authorityList = Arrays.asList(user.getRole());
        var authDto = new AuthenticationInfoDto();
        authDto.user = user.asDto();
        authDto.token.token = JwtAuth.createJwtTokenWithRealm(user.getUserName(), authorityList, jwtConf, 
                user.getRealm());
        return authDto;
    }

    /**
     * Writes the given authentication information into the http response as JSON. 
     * 
     * @param response The targeted response
     * @param authDto Information which will be the return content
     */
    private void setResponseValue(@Nonnull HttpServletResponse response, @Nonnull AuthenticationInfoDto authDto) {
        response.addHeader(jwtConf.getHeader(), jwtConf.getPrefix() + " " + authDto.token);
        response.setContentType("application/json; charset=UTF-8"); 
        try (var responseWriter = response.getWriter()) {
            String bodyDtoString = new ObjectMapper().writeValueAsString(authDto);
            responseWriter.write(bodyDtoString);
            responseWriter.flush();
        } catch (IOException e) {
            log.warn("Invalid json format");
        }
    }
}