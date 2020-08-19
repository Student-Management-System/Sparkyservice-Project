package net.ssehub.sparkyservice.api.user.transformation;

public class MissingDataException extends RuntimeException {
    private static final long serialVersionUID = -8538327422413216690L;
    private String message;
    
    public MissingDataException(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
