package net.ssehub.sparkyservice.api.auth.identity;

/**
 * Exception that a Realm definition was not found for a given identifier. This exception 
 * should be thrown when only a realm identifier is provided and it is not possible to transfer
 * it a {@link UserRealm} instance. 
 * 
 * @author marcel
 */
public class NoSuchRealmException extends RuntimeException {

    private static final long serialVersionUID = 5658059123974462396L;

    public NoSuchRealmException(String msg) {
        super(msg);
    }

    public NoSuchRealmException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
