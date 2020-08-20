package net.ssehub.sparkyservice.api.user.modification;

/**
 * UserEditException.
 * 
 * @author marcel
 */
public class UserEditException extends Exception {
    private static final long serialVersionUID = -6392177885876281701L;
    private String message;
    
    
    /**
     * Exception when a user edit fails.
     * 
     * @param message
     */
    public UserEditException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
