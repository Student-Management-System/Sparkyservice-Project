package net.ssehub.sparkyservice.api.auth.exceptions;

public class AccessViolationException extends Exception {
    private static final long serialVersionUID = 7347086477709067260L;
    private String message;
    
    public AccessViolationException(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
