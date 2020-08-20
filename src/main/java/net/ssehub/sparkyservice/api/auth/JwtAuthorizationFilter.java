package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.notNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.user.transformation.UserTransformerService;

/**
 * Filter which handles authorization with JWT token.
 * 
 * @author marcel
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthorizationFilter.class);
    private final JwtSettings confValues;
    private final @Nonnull Set<String> lockedJwtToken;
    private final UserTransformerService userTransformer;

    /**
     * JWT Authorization filter for paths which are configured in the authentication manager. 
     * It reads a JWT token from {@link JwtSettings#getHeader()} and tries to authorize the user. 
     * When the token is valid, the user is granted access to the requested a the user object is stored in the 
     * authentication object. 
     * 
     * @param authenticationManager
     * @param jwtConf - Contains all necessary JWT configurations
     * @param service - Used to extends the information from the JWT token to be sure, all information matches with 
     *                  the database
     */
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf, 
            UserTransformerService service) {
        this(authenticationManager, jwtConf, new HashSet<String>(), service);
    }

    /**
     * JWT Authorization filter for paths which are configured in the authentication manager. 
     * It reads a JWT token from {@link JwtSettings#getHeader()} and tries to authorize the user. 
     * When the token is valid, the user is granted access to the requested a the user object is stored in the 
     * authentication object. 
     * 
     * @param authenticationManager
     * @param jwtConf - Contains all necessary JWT configurations
     * @param service - Used to extends the information from the JWT token to be sure, all information matches with 
     *                  the database
     * @param lockedJwtToken - A set of token which are currently locked (they never can get access to the requested
     *                         resource)
     */
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf, 
            @Nullable Set<String> lockedJwtToken, UserTransformerService service) {
        super(authenticationManager);
        confValues = jwtConf;
        this.userTransformer = service;
        this.lockedJwtToken = notNull(Optional.ofNullable(lockedJwtToken).orElseGet(HashSet::new));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        var authentication = getAuthentication(request);
        if (authentication == null) {
            filterChain.doFilter(request, response);
            return;
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    /**
     * Creates an authentication token for spring with an JWT token which it extracts from the request.
     * 
     * @param request
     * @return Token object with the values of the JWT token
     */
    private @Nullable UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        var jwt = request.getHeader(confValues.getHeader());  
        Predicate<String> isNotLocked = token -> !lockedJwtToken.stream().anyMatch(token::contains); //wildcard
        Optional<UsernamePasswordAuthenticationToken> optTokenObj = Optional.ofNullable(jwt)
            .filter(isNotLocked)
            .map(this::getTokenObject);
        optTokenObj.ifPresentOrElse(
            token -> LOG.debug("Successful authorization with: {}", token),
            () -> LOG.debug("Token not was not valid {}", jwt)
        );
        return optTokenObj.orElse(null);
    }

    /**
     * Get information from JWT token. 
     * 
     * @param jwt
     * @return Token with content described at {@link JwtAuth#readJwtToken(String, String)}
     */
    private UsernamePasswordAuthenticationToken getTokenObject(@Nonnull String jwt) {
        UsernamePasswordAuthenticationToken tokenObject = null;
        try {
            tokenObject = JwtAuth.readJwtToken(jwt, confValues.getSecret(), userTransformer);
        } catch (JwtTokenReadException e1) {
            LOG.info("Non valid JWT Token was provided for authorization: {}", jwt);
        }
        return tokenObject;
    }
}

