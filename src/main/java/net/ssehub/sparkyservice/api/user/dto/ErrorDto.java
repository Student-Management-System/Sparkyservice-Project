package net.ssehub.sparkyservice.api.user.dto;

// checkstyle: stop visibility modifier check
/**
 * Immutable DTO for errors. This DTO is compatible with default spring error messages. 
 * 
 * @author marcel
 */
public final class ErrorDto {

    public final String timestamp;
    public final int status;
    public final String error;
    public final String messge;
    public final String path;

    /**
     * Default constructor for Error Messages.
     * 
     * @param timestamp - Time of the occured error
     * @param status - Status code to display
     * @param error - Error type
     * @param messge - Error message
     * @param path - The URL where the Error occured
     */
    public ErrorDto(String timestamp, int status, String error, String messge, String path) {
        super();
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.messge = messge;
        this.path = path;
    }
}
