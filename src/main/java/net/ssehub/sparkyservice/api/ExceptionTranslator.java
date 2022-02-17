package net.ssehub.sparkyservice.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import net.ssehub.sparkyservice.api.auth.exception.AuthenticationException;
import net.ssehub.sparkyservice.api.auth.exception.AuthorizationException;
import net.ssehub.sparkyservice.api.auth.jwt.JwtTokenReadException;
import net.ssehub.sparkyservice.api.user.dto.ErrorDto;
import net.ssehub.sparkyservice.api.user.extraction.MissingDataException;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

@RestControllerAdvice
public class ExceptionTranslator {

    
    /**
     * Exception and Error handler for this Controller Class. It produces a new informational ErrorDto based
     * on the thrown exception.
     * 
     * @param ex 
     * @return Informational ErrorDto which is comparable with the  default Spring Error Text
     */
    @ExceptionHandler({ AccessDeniedException.class, AuthorizationException.class })
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public ErrorDto handleUserEditException(Exception ex) {
        return new ErrorDtoBuilder().newError(ex.getMessage(), HttpStatus.FORBIDDEN, "").build();
    }
    
    /**
     * Exception handler to show BAD_REQUEST response to the user.
     * 
     * @param ex
     * @return Informational ErrorDto which is comparable with the  default Spring Error Text
     */
    @ExceptionHandler({ MissingDataException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorDto handleMissingDataExceptions(Exception ex) {
        return new ErrorDtoBuilder().newError(ex.getMessage(), HttpStatus.BAD_REQUEST, "").build();
    }
    
    /**
     * Excpetion handler for unauthorized request.
     * 
     * @param ex
     * @return DTO with information
     */
    @ExceptionHandler({ JwtTokenReadException.class, AuthenticationException.class })
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public ErrorDto handleJwtException(Exception ex) {
        return new ErrorDtoBuilder().newError("Missing or malformed Authorization header", HttpStatus.UNAUTHORIZED, "")
                .build();
    }
}
