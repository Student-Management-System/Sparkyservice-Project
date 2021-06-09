package net.ssehub.sparkyservice.api.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.ssehub.sparkyservice.api.user.dto.ErrorDto;

public class ErrorDtoBuilder {

    private HttpTimestamp timestamp = new HttpTimestamp();
    private String urlPath;
    private HttpStatus returnStatus;
    private String message;

    /**
     * Creates a new instance which creates customizable {@link ErrorDto} objects.
     */
    public ErrorDtoBuilder() {}

    public @Nonnull ErrorDtoBuilder newUnauthorizedError(@Nullable String path) {
        return newError(null, HttpStatus.UNAUTHORIZED, path);
    }

    public @Nonnull ErrorDtoBuilder newUnauthorizedError(@Nullable String customMessage, @Nullable String path) {
        return newError(message, HttpStatus.UNAUTHORIZED, path);
    }

    /**
     * Creates a new error message dto.
     * 
     * @param customMessage Own error message; when null, a default message is set
     * @param status The returned HttpStatus - this method builder does not the the response type!
     * @param path Served HTTP Path
     * @return this builder
     */
    public @Nonnull ErrorDtoBuilder newError(@Nullable String customMessage, 
            @Nonnull HttpStatus status, @Nullable String path) {
        message = customMessage;
        returnStatus = status;
        urlPath = path;
        return this;
    }

    /**
     * Builds a new ErrorDto object.
     * 
     * @return Immutable ErrorDto which is never null
     */
    public @Nonnull ErrorDto build() {
        if (message == null) {
            switch (returnStatus) {
            case FORBIDDEN:
                message = "Insufficient permission to access this location";
                break;
            case UNAUTHORIZED:
                message = "Protected path. Use authentication controller";
                break;
            default:
                message = "No message";
                break;
            }
        }
        return new ErrorDto(timestamp.toString(), returnStatus.value(), returnStatus.name(), message, urlPath);
    }

    /**
     * Overrides the default timestamp. Default value is the time where the builder
     * was initialized
     * 
     * @param timestamp
     * @return this
     */
    public @Nonnull ErrorDtoBuilder setTimestamp(@Nonnull HttpTimestamp timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public @Nonnull ErrorDtoBuilder setUrlPath(@Nullable String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    /**
     * Builds the error message as JSON String.
     * 
     * @return An {@link ErrorDto} serialized as JSON String
     */
    public String buildAsJson() {
        var dto = build();
        try {
            var mapper = new ObjectMapper();
            return mapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't create JSON String of ErrorDto");
        }
    }
}
