package net.ssehub.sparkyservice.api.storeduser;

public class UserNotFoundException extends Exception {
    private static final long serialVersionUID = -7715170940227356680L;
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
