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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import net.ssehub.sparkyservice.api.auth.jwt.JwtAuthTools;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenService;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;

/**
 * Filter which handles authorization with JWT token.
 * 
 * @author marcel
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthorizationFilter.class);
    private final JwtTokenService jwtService;

    /**
     * JWT Authorization filter for paths which are configured in the authentication manager. 
     * It reads a JWT token from {@link JwtSettings#getHeader()} and tries to authorize the user. 
     * When the token is valid, the user is granted access to the requested a the user object is stored in the 
     * authentication object. 
     * 
     * @param authenticationManager
     * @param service Jwt service used for decoding jwt tokens
     */
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtTokenService service) {
        this(authenticationManager, new HashSet<String>(), service);
    }

    /**
     * JWT Authorization filter for paths which are configured in the authentication manager. 
     * It reads a JWT token from {@link JwtSettings#getHeader()} and tries to authorize the user. 
     * When the token is valid, the user is granted access to the requested a the user object is stored in the 
     * authentication object. 
     * 
     * @param authenticationManager
     * @param service Jwt service used for decoding jwt tokens
     */
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, 
            @Nullable Set<String> lockedJwtToken, JwtTokenService service) {
        super(authenticationManager);
        this.jwtService = service;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        LOG.debug("Requested URI: {}", request.getRequestURI());
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
    private @Nullable Authentication getAuthentication(HttpServletRequest request) {
        String header = jwtService.getJwtConf().getHeader();
        var jwt = request.getHeader(header);
        Optional<Authentication> optTokenObj = Optional.ofNullable(jwt)
            .map(this::getAuthenticationFromJwt);
        optTokenObj.ifPresentOrElse(
            token -> LOG.debug("Successful authorization of: {}", token.getName()),
            () -> LOG.info("Denied access to token: {}", jwt)
        );
        return optTokenObj.orElse(null);
    }

    /**
     * Get information from JWT token. 
     * 
     * @param jwt
     * @return Token with content described at {@link JwtAuthTools#readJwtToken(String, String)}
     */
    private Authentication getAuthenticationFromJwt(@Nonnull String jwt) {
        Authentication authentication = null;
        try {
            authentication = jwtService.readToAuthentication(jwt);
        } catch (JwtTokenReadException e1) {
            LOG.debug("Non valid JWT Token was provided for authorization: {}", jwt);
        }
        return authentication;
    }
}

