package net.ssehub.sparkyservice.api.persistence;

import org.springframework.transaction.TransactionException;

/**
 * Exception when no transaction unit for an object exists.
 * 
 * @author marcel
 */
public class NoTransactionUnitException extends TransactionException {

    private static final long serialVersionUID = 502098601047556845L;

    /**
     * Exception when no transaction unit for an object exists.
     * @param message
     */
    public NoTransactionUnitException(String message) {
        super(message);
    }

    /**
     * Exception when no transaction unit for an object exists.
     * @param message
     * @param ex
     */
    public NoTransactionUnitException(String message, Throwable ex) {
        super(message, ex);
    }
}
