package net.ssehub.sparkyservice.api.auth;

import java.io.IOException;

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
import net.ssehub.sparkyservice.api.conf.ConfigurationValues;

public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);
    private final ConfigurationValues confValues;
    
    public JwtAuthorizationFilter(AuthenticationManager authenticationManager, ConfigurationValues jwtConf) {
        super(authenticationManager);
        confValues = jwtConf;
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

    private @Nullable UsernamePasswordAuthenticationToken getAuthentication(@Nullable HttpServletRequest request) {
        if (request != null) {
            var token = request.getHeader(confValues.getJwtTokenHeader());            
            if (!StringUtils.isEmpty(token) && token.startsWith(confValues.getJwtTokenPrefix())) {
                try {
                    return JwtAuth.readJwtToken(token, confValues.getJwtSecret());
                } catch (ExpiredJwtException exception) {
                    log.warn("Request to parse expired JWT : {} failed : {}", token, exception.getMessage());
                } catch (UnsupportedJwtException exception) {
                    log.warn("Request to parse unsupported JWT : {} failed : {}", token, exception.getMessage());
                } catch (MalformedJwtException exception) {
                    log.warn("Request to parse invalid JWT : {} failed : {}", token, exception.getMessage());
                } catch (SignatureException exception) {
                    log.warn("Request to parse JWT with invalid signature : {} failed : {}", token, exception.getMessage());
                } catch (IllegalArgumentException exception) {
                    log.warn("Request to parse empty or null JWT : {} failed : {}", token, exception.getMessage());
                }
            }
        }
        return null;
    }
}

