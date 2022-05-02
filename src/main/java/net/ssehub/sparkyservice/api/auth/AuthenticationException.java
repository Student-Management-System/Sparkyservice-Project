package net.ssehub.sparkyservice.api.auth;

/**
 * .
 * @author marcel
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = -2967639679655487549L;

    /**
     * .
     */
    public AuthenticationException() {
    }
    
    /**
     * .
     * @param throwable
     */
    public AuthenticationException(Throwable throwable) {
        super(throwable);
    }
}