package net.ssehub.sparkyservice.api.user.storage;

import org.springframework.security.core.AuthenticationException;

public class UserNotFoundException extends AuthenticationException {
    private static final long serialVersionUID = -7715170940227356680L;
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
