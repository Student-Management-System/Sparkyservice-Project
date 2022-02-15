package net.ssehub.sparkyservice.api.auth.exception;

import net.ssehub.sparkyservice.api.auth.Identity;

/**
 * .
 * @author marcel
 */
public class AuthorizationException extends RuntimeException {

    private static final long serialVersionUID = -1077046393444428415L;
    
    private Identity ident;
    
    /**
     * Authorization problem.
     * 
     * @param ident The user identity which caused the problem.
     */
    public AuthorizationException(Identity ident) {
        super(ident.asUsername());
        this.ident = ident;
    }
    
    /**
     * The user which caused an authorization exception.
     * 
     * @return may be null
     */
    public Identity getCauseUser() {
        return this.ident;
    }
}