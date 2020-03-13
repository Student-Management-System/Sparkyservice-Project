package net.ssehub.sparkyservice.api.storeduser;

public class MissingDataException extends Exception {
    private static final long serialVersionUID = -8538327422413216690L;
    private String message;
    
    public MissingDataException(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
