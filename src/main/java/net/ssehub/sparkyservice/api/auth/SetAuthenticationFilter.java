package net.ssehub.sparkyservice.api.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter which handles checks authentication information with JWT.
 * 
 * @author marcel
 */
public class SetAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SetAuthenticationFilter.class);
    private final AuthenticationConverter converter;

    /**
     * JWT authentication filter for paths which are configured in the
     * authentication manager.
     * 
     * @param authenticationManager
     * @param authService
     */
    public SetAuthenticationFilter(AuthenticationConverter converter) {
        this.converter = converter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        LOG.debug("Requested URI: {}", request.getRequestURI());
        try {
            var authentication = converter.convert(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);            
        } catch (Exception e) {
            // dont log verbose here! Don't make custom exception handling. This filter is called every request.
            LOG.debug(request.getRequestURI() + " could not authentication with JWT on request", e);
            filterChain.doFilter(request, response);
        }
    }

}
