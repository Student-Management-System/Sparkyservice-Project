package net.ssehub.sparkyservice.api.user.storage;

import org.springframework.security.core.AuthenticationException;

/**
 *  Provides an exception for cases where a searched user is not found.
 * 
 * @author marcel
 */
public class UserNotFoundException extends AuthenticationException {
    private static final long serialVersionUID = -7715170940227356680L;
    
    /**
     * When a user is not found (typically in a persistent storage). 
     * 
     * @param message
     */
    public UserNotFoundException(String message) {
        super(message);
    }
}
