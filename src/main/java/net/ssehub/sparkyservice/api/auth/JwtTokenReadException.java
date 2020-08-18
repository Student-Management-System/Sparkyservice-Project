package net.ssehub.sparkyservice.api.auth;

public class JwtTokenReadException extends Exception {
    private static final long serialVersionUID = 4607607061910826682L;
    private String message;
    
    public JwtTokenReadException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
