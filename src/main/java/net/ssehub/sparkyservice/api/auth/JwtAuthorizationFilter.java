package net.ssehub.sparkyservice.api.auth;

import static net.ssehub.sparkyservice.api.util.NullHelpers.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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
import org.springframework.util.StringUtils;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;

/**
 * Filter which handles authorization with JWT token.
 * 
 * @author marcel
 */
public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthorizationFilter.class);
    private final JwtSettings confValues;
    private final @Nonnull Set<String> lockedJwtToken;

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf) {
        this(authenticationManager, jwtConf, new HashSet<String>());
    }

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, JwtSettings jwtConf, 
            @Nullable Set<String> lockedJwtToken) {
        super(authenticationManager);
        confValues = jwtConf;
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
    private @Nullable UsernamePasswordAuthenticationToken getAuthentication(@Nullable HttpServletRequest request) {
        UsernamePasswordAuthenticationToken tokenObject = null;
        if (request != null) {
            var token = request.getHeader(confValues.getHeader());            
            if (!StringUtils.isEmpty(token) && token.startsWith(confValues.getPrefix())) {
                try {
                    boolean userIsDisabled = lockedJwtToken.stream().anyMatch(e -> token.contains(e)); //wildcard possible
                    if (userIsDisabled) {
                        LOG.warn("Locked token tried to authorize: {}", token);
                    } else {
                        tokenObject = JwtAuth.readJwtToken(token, confValues.getSecret())
                                .orElseThrow(IllegalArgumentException::new);
                    }
                } catch (ExpiredJwtException exception) {
                    LOG.warn("Request to parse expired JWT : {} failed : {}", token, exception.getMessage());
                } catch (UnsupportedJwtException exception) {
                    LOG.warn("Request to parse unsupported JWT : {} failed : {}", token, exception.getMessage());
                } catch (MalformedJwtException exception) {
                    LOG.warn("Request to parse invalid JWT : {} failed : {}", token, exception.getMessage());
                } catch (SignatureException exception) {
                    LOG.warn("Request to parse JWT with invalid signature : {} failed : {}", 
                            token, exception.getMessage());
                } catch (IllegalArgumentException exception) {
                    LOG.warn("Request to parse empty or null JWT : {} failed : {}", token, exception.getMessage());
                }
            }
        }
        return tokenObject;
    }
}

