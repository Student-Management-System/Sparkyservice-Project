package net.ssehub.sparkyservice.api.auth;

/**
 * Exception for JWT token read errors.
 * 
 * @author marcel
 */
public class JwtTokenReadException extends Exception {

    private static final long serialVersionUID = 4607607061910826682L;
    private String message;

    /**
     * Exception when a JWT token could not read.
     * 
     * @param message
     */
    public JwtTokenReadException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
