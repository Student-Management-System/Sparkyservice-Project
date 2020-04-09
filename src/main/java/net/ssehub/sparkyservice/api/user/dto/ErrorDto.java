package net.ssehub.sparkyservice.api.user.dto;

public final class ErrorDto {

    public final String timestamp;
    public final int status;
    public final String error;
    public final String messge;
    public final String path;

    public ErrorDto(String timestamp, int status, String error, String messge, String path) {
        super();
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.messge = messge;
        this.path = path;
    }
}
