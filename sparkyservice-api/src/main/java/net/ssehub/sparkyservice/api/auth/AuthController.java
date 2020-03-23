package net.ssehub.sparkyservice.api.auth;


import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import net.ssehub.sparkyservice.api.conf.ConfigurationValues;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.storeduser.UserNotFoundException;
/**
 * Controller for authentication reachable under /auth
 * @author marcel
 */
@RestController
public class AuthController {
    
    @Autowired
    private ConfigurationValues confValues;
    
    public class AuthenticationFailedException extends Exception {
        private static final long serialVersionUID = 7472411757860220245L;
    }
    @Resource(name="authenticationManager")
    private AuthenticationManager authManager;
    
    /**
     * This method does nothing. The method header is important to let swagger list this authentication method.
     * The authentication is handled through @link {@link JwtAuthenticationFilter} which listens on the same
     * path than this method.
     * @param username Username of the user
     * @param password Password of the user
     * @return HTTP body and sets JWT Token inside http header
     */
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH) 
    public String authenticate(@RequestParam(value = "username") @NotNull String username, 
                        @RequestParam(value = "password") @NotNull String password) {
        throw new UnsupportedOperationException();
    }
    
    @PostMapping(value = ControllerPath.GLOBAL_PREFIX + "/auth") 
    public void auth(@RequestParam(value = "username") @NotNull String username, 
                        @RequestParam(value = "password") @NotNull String password,
                        HttpServletResponse response, HttpServletRequest request) {
        try {
            UsernamePasswordAuthenticationToken userDetails = JwtAuth.
                    extractCredentialsFromHttpRequest(request);
            var authentication = this.authManager.authenticate(userDetails);
            var token = JwtAuth.createJwtFromAuthentication(authentication, confValues);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            response.addHeader(confValues.getJwtTokenHeader(), 
                    confValues.getJwtTokenPrefix() + token);
        } catch (Exception e) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }
    }
    
    /**
     * Handler for an {@link UserNotFoundException}. 
     * @param ex
     * @return
     */
    @ExceptionHandler(value = UserNotFoundException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public String handleUserNotFoundException(UserNotFoundException ex) {
        return this.handleError();
    }
    
    @ExceptionHandler(value = AuthenticationFailedException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public String handleAuthenticationFailedException(AuthenticationFailedException ex) {
        return this.handleError();
    }
    
    public String handleError() {
        return "Error";
    }
}
