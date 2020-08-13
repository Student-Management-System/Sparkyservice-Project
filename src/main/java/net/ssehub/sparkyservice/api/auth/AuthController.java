package net.ssehub.sparkyservice.api.auth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.ssehub.sparkyservice.api.conf.ConfigurationValues.JwtSettings;
import net.ssehub.sparkyservice.api.conf.ControllerPath;
import net.ssehub.sparkyservice.api.user.UserTransformer;
import net.ssehub.sparkyservice.api.user.dto.CredentialsDto;
import net.ssehub.sparkyservice.api.user.dto.ErrorDto;
import net.ssehub.sparkyservice.api.user.dto.TokenDto;
import net.ssehub.sparkyservice.api.user.exceptions.MissingDataException;
import net.ssehub.sparkyservice.api.user.exceptions.UserNotFoundException;
import net.ssehub.sparkyservice.api.util.ErrorDtoBuilder;

/**
 * Controller for authentication.
 * 
 * @author Marcel
 */
@RestController
@Tag(name = "auth-controller", description = "Controller for realm authentication with JWT")
public class AuthController {

    @Autowired
    private ServletContext servletContext;
    @Autowired
    private UserTransformer transformator;
    @Autowired
    private JwtSettings jwtConf;

    /**
     * This method does nothing. The method header is important to let swagger list
     * this authentication method. The authentication is handled through
     * {@link JwtAuthenticationFilter} which listens on the same path than this
     * method.
     * 
     * @param credentials - Contains username and password
     * @return Information like JWT token when user was successfully authenticated
     */
    @Operation(summary = "Authentication / Login", 
            description = "Authenticates the user and sets a JWT into the authorization header")
    @PostMapping(value = ControllerPath.AUTHENTICATION_AUTH)
    @ApiResponses(value = { 
            @ApiResponse(responseCode = "200", description = "Authentication success"),
            
            @ApiResponse(responseCode = "403", description = "Authentication failed", 
                content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = ErrorDto.class)))
    })
    public AuthenticationInfoDto authenticate(@Nonnull @NotNull @Valid CredentialsDto credentials) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the user is authenticated with a given JWT Token. This controller
     * is not protected through spring security in order to provide better
     * information about what went wrong.
     * 
     * @param auth - Injected through spring if the user is logged in - holds
     *             authentication information
     * @param request - Provided by Spring
     * @return user information which are stored in the JWT token
     * @throws UserNotFoundException
     * @throws MissingDataException
     */
    @Operation(description = "Checks the authentication state of the users authorization header and returns all "
            + " user informations which belongs to the user",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = ControllerPath.AUTHENTICATION_CHECK)
    @ApiResponses(value = { 
            @ApiResponse(responseCode = "200", description = "Authentication status is good"),
            @ApiResponse(responseCode = "403", description = "Not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorDto.class))) })
    public AuthenticationInfoDto checkTokenAuthenticationStatus(@Nullable Authentication auth,
            HttpServletRequest request) throws MissingDataException {
        if (auth == null) { // check what went wrong
            var jwtToken = request.getHeader(jwtConf.getHeader());
            if (!StringUtils.isEmpty(jwtToken) && jwtToken.startsWith(jwtConf.getPrefix())) {
                JwtAuth.readJwtToken(jwtToken, jwtConf.getSecret()); // should throw something
            }
            throw new AuthenticationException();
        }
        var user = transformator.extendFromAuthentication(auth);
        var dto = new AuthenticationInfoDto();
        dto.user = user.asDto();
        if (auth.getCredentials() instanceof TokenDto) {
            dto.token = (TokenDto) auth.getCredentials();
        }
        return dto;
    }

    /*
     * Only supports jwtTokens which was created through the projects own
     * authentication filter. Mocking users with spring during integration tests are
     * not supported here.
     */
    /**
     * Checks if a given token is valid. Validity means a non expired token which contains all information
     * which are necessary to proceed with it in the application. When the token is valid, it can be used as 
     * authentication token for the whole application.
     * 
     * @param jwtToken - The token which should be verified
     * @return The stored information in the token
     * @throws MissingDataException - Is thrown when the data of the token is not complete
     */
    @Operation(description = "Prints the validity status of a given token",
            security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping(value = ControllerPath.AUTHENTICATION_VERIFY)
    public AuthenticationInfoDto verifyTokenValidity(@NotNull @Nonnull String jwtToken) throws MissingDataException {
        if (!StringUtils.isEmpty(jwtToken) && jwtToken.startsWith(jwtConf.getPrefix())) {
            var auth = JwtAuth.readJwtToken(jwtToken, jwtConf.getSecret())
                    .orElseThrow(AuthenticationException::new); // should throw something
            var user = transformator.extendFromAuthentication(auth);
            var dto = new AuthenticationInfoDto();
            dto.user = user.asDto();
            dto.token = (TokenDto) auth.getCredentials();
            return dto;
        }
        throw new AuthenticationException();
    }
    
    /**
     * Exception and Error handler for this Controller Class. It produces a new informational ErrorDto based
     * on the thrown exception.
     * 
     * @param ex - Can be  AccessViolationException.class, MissingDataException.class, UserNotFoundException.class 
     * @return Informational ErrorDto which is comparable with the  default Spring Error Text
     */
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ExceptionHandler({ AuthenticationException.class, MissingDataException.class, UserNotFoundException.class })
    public ErrorDto handleUserEditException(Exception ex) {
        return new ErrorDtoBuilder().newUnauthorizedError(ex.getMessage(), servletContext.getContextPath()).build();
    }

    /**
     * Exception and Error handler for this Controller Class. It produces a new informational ErrorDto based
     * on the thrown exception.
     * 
     * @param ex - Can be AuthenticationException.class 
     * @return Informational ErrorDto which is comparable with the  default Spring Error Text
     */
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({ AuthenticationException.class })
    public ErrorDto handleAuthenticationException(AuthenticationException ex) {
        return new ErrorDtoBuilder().newUnauthorizedError(ex.getMessage(), servletContext.getContextPath()).build();
    }

    /*
     * The catch all method avoids leaking internal error messages to the user. 
     */
    /**
     * Exception and Error handler for this Controller Class. It produces a new informational ErrorDto based
     * on the thrown exception.
     * This method catches any exception which is not already catched by another handler method. 
     * 
     * @param ex - Any excetpion.
     * @return Informational ErrorDto which is comparable with the  default Spring Error Text
     */
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler({ Exception.class })
    public ErrorDto handleException(Exception  ex) {
        // TODO build log
        return new ErrorDtoBuilder().newUnauthorizedError(null, servletContext.getContextPath()).build();
    }
}
