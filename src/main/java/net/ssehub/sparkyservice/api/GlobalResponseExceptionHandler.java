package net.ssehub.sparkyservice.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.CommunicationException;
import org.springframework.ldap.UncategorizedLdapException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.exception.AuthorizationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

@RestControllerAdvice
public class GlobalResponseExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(GlobalResponseExceptionHandler.class);

    @ExceptionHandler(value = { IllegalArgumentException.class, IllegalStateException.class })
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        var status = HttpStatus.BAD_REQUEST;
        var responseDto = new ErrorDtoBuilder(ex.getMessage(), status).build();
        return handleExceptionInternal(ex, responseDto, new HttpHeaders(), status, request);
    }
        
    @ExceptionHandler(value = { AuthenticationException.class, JwtTokenReadException.class})
    protected ResponseEntity<Object> handleMissingOrWrongAuthentication(RuntimeException ex, WebRequest request) {
        var status = HttpStatus.UNAUTHORIZED;
        var responseDto = new ErrorDtoBuilder("No authentication header provided." + ex.getMessage(), status).build();
        return handleExceptionInternal(ex, responseDto, new HttpHeaders(), status, request);
    }
    
    @ExceptionHandler(value = { BadCredentialsException.class } )
    protected ResponseEntity<Object> handleBadCredentials(RuntimeException ex, WebRequest request) {
        var status = HttpStatus.UNAUTHORIZED;
        var responseDto = new ErrorDtoBuilder(ex.getMessage(), status).build();
        return handleExceptionInternal(ex, responseDto, new HttpHeaders(), status, request);
    }
    
    
    @ExceptionHandler(value = { AuthorizationException.class, AccessDeniedException.class})
    protected ResponseEntity<Object> handleAuthorization(RuntimeException ex, WebRequest request) {
        var status = HttpStatus.FORBIDDEN;
        var responseDto = new ErrorDtoBuilder(ex.getMessage(), status).build();
        return handleExceptionInternal(ex, responseDto, new HttpHeaders(), status, request);
    }
    
    @ExceptionHandler(value = { CommunicationException.class, UncategorizedLdapException.class })
    protected ResponseEntity<Object> handleLdapConnectionError(org.springframework.ldap.CommunicationException ex, 
            WebRequest request) {
        var status = HttpStatus.UNAUTHORIZED;
        var responseDto = new ErrorDtoBuilder("Could not complete authentication request.", status).build();
        LOG.warn("Could not complete authentication request due do a LDAP connection error"
                + " (most likely due to incorrect configuration). "
                + "This may effect affect other realms below the authentication chain.");
        return handleExceptionInternal(ex, responseDto, new HttpHeaders(), status, request);
    }
}