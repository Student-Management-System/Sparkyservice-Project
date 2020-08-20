package net.ssehub.sparkyservice.api.auth;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.modification.UserModificationService;
import net.ssehub.sparkyservice.api.user.storage.UserStorageService;
import net.ssehub.sparkyservice.api.user.transformation.UserTransformerService;

/**
 * A Filter which handles all authentication requests and actually handles the login.
 * @author marcel
 */
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JwtSettings jwtConf;
    private final UserStorageService userService;
    private final Logger log = LoggerFactory.getLogger(JwtAuth.class);

    /**
     * Constructor for the general Authentication filter. In most cases filters are set in the spring security 
     * configuration. 
     * 
     * @param authenticationManager
     * @param jwtConf
     * @param userSerivce
     * @param transformator
     */
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf,
                                   UserStorageService userSerivce, UserTransformerService transformator) {
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
        var authentication = authenticationManager.authenticate(userDetails);
        assertSparkyUser(authentication);
        return authentication;
        // in memory auth: principal=MemoryUser
        // ldap auth: principal=LdapUser
        // local: Principal=LocalUserDetails (vermutung)
        //vermutlich beim authorization im principal:
            //  sparkysauthprincipal
            // oder spring user
    }

    /**
     * {@inheritDoc}.
     * The principal (accessable through {@link Authentication#getPrincipal()} of this authentication always contains 
     * the authenticated {@link SparkyUser}.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain, Authentication authentication) {

        log.info("Successful authentication with JWT");
        var user = Optional.of(authentication)
            .map(a -> a.getPrincipal())
            .map(SparkyUser.class::cast);
        user.filter(u -> !userService.isUserInStorage(u))
            .ifPresent(userService::commit);
        user.map(this::buildAuthenticatioInfoFromUser)
            .ifPresent(dto -> setResponseValue(response, dto));
    }

    /**
     * Builds an authentication DTO with the information provided by user object.
     * 
     * @param user - object which contains all necessary information.
     * @return AuthenticationDTO currently without {@link TokenDto#expiration}
     */
    private @Nonnull AuthenticationInfoDto buildAuthenticatioInfoFromUser(@Nonnull SparkyUser user) {
        var authDto = new AuthenticationInfoDto();
        authDto.user = UserModificationService.from(user.getRole()).asDto(user);
        authDto.token.token = JwtAuth.createJwtToken(user, jwtConf);
        return authDto;
    }

    /**
     * Writes the given authentication information into the http response as JSON. 
     * 
     * @param response The targeted response
     * @param authDto Information which will be the return content
     */
    private void setResponseValue(@Nonnull HttpServletResponse response, @Nonnull AuthenticationInfoDto authDto) {
        response.addHeader(jwtConf.getHeader(), jwtConf.getPrefix() + " " + authDto.token.token);
        response.setContentType("application/json; charset=UTF-8"); 
        try (var responseWriter = response.getWriter()) {
            String bodyDtoString = new ObjectMapper().writeValueAsString(authDto);
            responseWriter.write(bodyDtoString);
            responseWriter.flush();
        } catch (IOException e) {
            log.warn("Authentication Header not written: Invalid json format.");
        }
    }

    /**
     * Technically it could happen that an administrator configures spring to not return a supported implementation.
     * This is a check at runtime for this.
     * 
     * @param auth - Current authentication object where the principal is checked
     */
    private static void assertSparkyUser(Authentication auth) {
        Optional.of(auth)
            .map(a -> a.getPrincipal())
            .filter(SparkyUser.class::isInstance)
            .map(SparkyUser.class::cast)
            .orElseThrow(() -> new RuntimeException("Spring authentication didn't provide a "
                    + "valid authentication object after authentication."));
    }
}