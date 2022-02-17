package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.io.IOException;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthReader;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.user.SparkyUser;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;

/**
 * A Filter which handles all authentication requests and actually handles the login.
 * @author marcel
 */
public class DoAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(DoAuthenticationFilter.class);

    private final ObjectMapper jacksonObjectMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtAuthReader jwtReader;
    private final JwtTokenService jwtService;
    private final HandlerExceptionResolver resolver;

    /**
     * Constructor for the general Authentication filter. In most cases filters are set in the spring security 
     * configuration. 
     * 
     * @param jwtService
     * @param authenticationManager
     * @param jwtReader
     * @param springOM The JSON object mapper used by spring for the REST interfaces.
     */
    public DoAuthenticationFilter(AuthenticationManager authenticationManager, JwtAuthReader jwtReader, 
            JwtTokenService jwtService, ObjectMapper springOM, HandlerExceptionResolver resolver) {
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl(ConfigurationValues.AUTH_LOGIN_URL);
        this.jwtReader = jwtReader;
        this.resolver = resolver;
        this.jacksonObjectMapper = springOM;
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        var userDetails = extractCredentialsFromHttpRequest(request);
        var authentication = authenticationManager.authenticate(userDetails);
        assertSparkyUser(authentication);
        return authentication;
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

    /**
     * {@inheritDoc}.
     * The principal (accessible through {@link Authentication#getPrincipal()} of this authentication always contains 
     * the authenticated {@link SparkyUser}.
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain, Authentication authentication) {
        LOG.info("Successful authentication: {}", authentication.getName());
        try {
            var user = notNull(Optional.of((SparkyUser) authentication.getPrincipal()).orElseThrow());
            var dto = createToken(user);
            setResponseValue(notNull(response), dto);
        } catch (Exception e) {
            LOG.error("Could not complete authentication request (authentication attempt was successful)", e);
            resolver.resolveException(request, response, null, e);
        }
    }

    /**
     * Creates an DTO which holds all information (authenticated) user and generates an JWT
     * token for this user. The generated token can be used for authorization. 
     * 
     * @param user
     * @return DTO with user information and generated JWT token
     */
    @Nonnull
    private AuthenticationInfoDto createToken(@Nonnull SparkyUser user) {
        String jwt = jwtService.createFor(user);
        try {
            return jwtReader.createAuthenticationInfoDto(jwt, user);
        } catch (JwtTokenReadException e) {
            throw new RuntimeException("Could not create Token. Maybe the server is misconfigured");
        }
    }
    
    /**
     * Writes the given authentication information into the http response as JSON. 
     * 
     * @param response The targeted response
     * @param authDto Information which will be the return content
     */
    private void setResponseValue(@Nonnull HttpServletResponse response, @Nonnull AuthenticationInfoDto authDto) {
        String httpHeader = jwtService.getJwtConf().getHeader();
        String jwtPrefix = jwtService.getJwtConf().getPrefix();
        response.addHeader(httpHeader, jwtPrefix + " " + authDto.token.token);
        response.setContentType("application/json; charset=UTF-8"); 
        try (var responseWriter = response.getWriter()) {
            String bodyDtoString = jacksonObjectMapper.writeValueAsString(authDto);
            responseWriter.write(bodyDtoString);
            responseWriter.flush();
        } catch (IOException e) {
            LOG.warn("Authentication Header not written: Invalid json format.");
        }
    }

    /**
     * Method reads the {@link CredentialsDto} from a given request and transform
     * them into a AuthenticationToken.
     * 
     * @param request
     * @return contains the username and password used for authentication
     */
    private static @Nonnull UsernamePasswordAuthenticationToken extractCredentialsFromHttpRequest(
            HttpServletRequest request) {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        boolean passwordAvailable = password != null && !password.isBlank();
        LOG.debug("[HTTP Parameter] Username: " + username + " | Password available: " + passwordAvailable);
        if (username == null && password == null) {
            try {
                CredentialsDto cred = new ObjectMapper().readValue(request.getInputStream(), CredentialsDto.class);
                username = cred.username.trim();
                password = cred.password;
                boolean avail = password != null && !password.isBlank();
                LOG.debug("[HTTP Body] Username: " + username + " | Password available: " + avail);
            } catch (MismatchedInputException e) {
                LOG.debug("Credentials not avaiable in requests input stream");
                // do nothing - is thrown on invalid values like null
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
        return new UsernamePasswordAuthenticationToken(username, password);
    }
}