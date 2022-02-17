package net.ssehub.sparkyservice.api.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Filter which handles checks authentication information with JWT.
 * 
 * @author marcel
 */
public class SetAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Logger LOG = LoggerFactory.getLogger(SetAuthenticationFilter.class);
    private final AuthenticationService authService;

    /**
     * JWT authentication filter for paths which are configured in the
     * authentication manager.
     * 
     * @param authenticationManager
     * @param authService
     */
    public SetAuthenticationFilter(AuthenticationManager authenticationManager, AuthenticationService authService) {
        super(authenticationManager);
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        LOG.debug("Requested URI: {}", request.getRequestURI());
        try {
            var authentication = authService.extractAuthentication(request);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);            
        } catch (Exception e) {
            // dont log verbose here! Don't make custom exception handling. This filter is called every request.
            LOG.debug(request.getRequestURI() + " could not authentication with JWT on request", e);
            filterChain.doFilter(request, response);
        }
    }

}
