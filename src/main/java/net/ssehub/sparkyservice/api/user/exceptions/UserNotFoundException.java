package net.ssehub.sparkyservice.api.user.exceptions;

public class UserNotFoundException extends Exception {
    private static final long serialVersionUID = -7715170940227356680L;
    
    public UserNotFoundException() {}
    public UserNotFoundException(String message) {
        super(message);
    }
}
