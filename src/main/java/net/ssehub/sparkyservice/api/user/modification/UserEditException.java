package net.ssehub.sparkyservice.api.user.modification;

public class UserEditException extends Exception {
    private static final long serialVersionUID = -6392177885876281701L;
    private String message;
    
    public UserEditException(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
