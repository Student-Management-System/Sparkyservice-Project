package net.ssehub.sparkyservice.api.user.transformation;

/**
 * MissingDateException.
 * 
 * @author marcel
 */
public class MissingDataException extends RuntimeException {
    private static final long serialVersionUID = -8538327422413216690L;
    private String message;

    /**
     * Exception indicates that some information are missing to move on 
     * with the current process.
     */
    public MissingDataException() {
        
    }

    /**
     * Exception indicates that some information are missing to move on 
     * with the current process. The missing date should be in the message.
     * 
     * @param message
     */
    public MissingDataException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
